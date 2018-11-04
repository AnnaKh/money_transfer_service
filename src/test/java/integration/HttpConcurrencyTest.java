package integration;

import com.mashape.unirest.http.HttpResponse;
import entrypoint.MoneyTransferFactory;
import model.Account;
import model.AccountSerializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class HttpConcurrencyTest {


    private static final int TRANSFER_COUNT = 5_000;
    private static final int ACCOUNT_COUNT = 10;
    private static final String URL_MAIN = "http://127.0.0.1:10001/accounts/";

    private final Random random = new Random();
    private final AccountSerializer accountSerializer = new AccountSerializer();
    private final MoneyTransferFactory moneyTransferFactory = new MoneyTransferFactory();


    @Before
    public void setUp() {
        moneyTransferFactory.launch("test_config.yaml");
    }

    @Test
    public void concurrencyTest() throws Exception {
        List<String> accounts = new ArrayList<>();
        double commonAmount = 0.;
        double amountForAccount = TRANSFER_COUNT;
        for (int i = 0; i < ACCOUNT_COUNT; i++) {
            commonAmount += amountForAccount;
            accounts.add(createAndRefillAccount(amountForAccount));
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        List<Future<HttpResponse<String>>> responses = random.ints(TRANSFER_COUNT, 0, 100)
                .boxed()
                .map(amount -> (Callable<HttpResponse<String>>) () -> {
                    int id = random.nextInt(accounts.size());
                    String source = accounts.get(id);
                    String destination = accounts.get((id + 1) % accounts.size());
                    return TestUtils.transferMoney(source, destination, amount, URL_MAIN);
                })
                .map(futureCallable -> {
                    try {
                        return threadPool.submit(futureCallable);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
        for (Future<HttpResponse<String>> response : responses) {
            assertEquals(response.get().getStatus(), 200);
        }

        BigDecimal resultAmount = accounts.stream()
                .map(id -> {
                    try {
                        return TestUtils.getAccount(id, URL_MAIN, accountSerializer).getBalance();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(resultAmount, BigDecimal.valueOf(commonAmount));
    }

    private String createAndRefillAccount(double amount) {
        try {
            Account account = new Account();
            account.setBalance(BigDecimal.ZERO);
            account.setName("Jack");
            Account acc1 = TestUtils.createAccount(account, URL_MAIN, accountSerializer);
            TestUtils.updateBalance(amount, URL_MAIN, acc1.getId());
            return acc1.getId();
        } catch (Exception e) {
            throw new RuntimeException("Incorrect creating/updating");
        }
    }


    @After
    public void tearDown() {
        moneyTransferFactory.stop();
    }
}
