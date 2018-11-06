package integration;

import com.mashape.unirest.http.HttpResponse;
import entrypoint.MoneyTransferFactory;
import model.Account;
import model.AccountSerializer;
import org.junit.*;
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
import static org.junit.Assert.assertNotNull;


public class HttpApiTest {

    private static final String URL_MAIN = "http://127.0.0.1:10001/accounts/";
    private static final MoneyTransferFactory moneyTransferFactory = new MoneyTransferFactory();

    private final AccountSerializer accountSerializer = new AccountSerializer();
    private static final int TRANSFER_COUNT = 5_000;
    private static final int ACCOUNT_COUNT = 10;

    private final Random random = new Random();

    @BeforeClass
    public static void setUp() {
        moneyTransferFactory.launch("config.yaml");
    }

    @AfterClass
    public static void tearDown() {
        moneyTransferFactory.stop();
    }

    @Test
    public void testCreateAccount() throws Exception {
        Account account = new Account();
        account.setName("Mary");
        Account accountFromServer = HttpTestUtils.createAccount(account, URL_MAIN, accountSerializer);
        assertEquals(accountFromServer.getName(), "Mary");
        assertEquals(accountFromServer.getBalance(), BigDecimal.ZERO);
        assertNotNull(accountFromServer.getId());
    }

    @Test
    public void testUpdateBalanceSuccess() throws Exception {
        Account account = new Account();
        account.setName("Mary");
        Account accountFromServer = HttpTestUtils.createAccount(account, URL_MAIN, accountSerializer);
        HttpResponse<String> response = HttpTestUtils.updateBalance(100, URL_MAIN, accountFromServer.getId());
        assertEquals(200, response.getStatus());

        Account updatedAccount = HttpTestUtils.getAccount(accountFromServer.getId(), URL_MAIN, accountSerializer);
        assertEquals(0, new BigDecimal(100).compareTo(updatedAccount.getBalance()));
    }

    @Test
    public void testUpdateBalanceToNegativeFail() throws Exception {
        Account account = new Account();
        account.setName("Mary");
        Account accountFromServer = HttpTestUtils.createAccount(account, URL_MAIN, accountSerializer);
        HttpResponse<String> response  = HttpTestUtils.updateBalance(-100, URL_MAIN, accountFromServer.getId());
        assertEquals(422, response.getStatus());

        Account updatedAccount = HttpTestUtils.getAccount(accountFromServer.getId(), URL_MAIN, accountSerializer);
        assertEquals(0, BigDecimal.ZERO.compareTo(updatedAccount.getBalance()));
    }

    @Test
    public void testTransferMoneySuccess() throws Exception {
        Account account1 = new Account();
        account1.setName("Alice");
        Account accountFromServer1 = HttpTestUtils.createAccount(account1, URL_MAIN, accountSerializer);

        Account account2 = new Account();
        account2.setName("Bob");
        Account accountFromServer2 = HttpTestUtils.createAccount(account2, URL_MAIN, accountSerializer);

        HttpTestUtils.updateBalance(5000, URL_MAIN, accountFromServer2.getId());


        HttpResponse<String> result = HttpTestUtils.transferMoney(accountFromServer2.getId(),
                accountFromServer1.getId(), 1000.,
                URL_MAIN);
        assertEquals(200, result.getStatus());

        Account updatedAccountAlice = HttpTestUtils.getAccount(accountFromServer1.getId(), URL_MAIN, accountSerializer);
        assertEquals(0, new BigDecimal(1000).compareTo(updatedAccountAlice.getBalance()));

        Account updatedAccountBob = HttpTestUtils.getAccount(accountFromServer2.getId(), URL_MAIN, accountSerializer);
        assertEquals(0, new BigDecimal(4000).compareTo(updatedAccountBob.getBalance()));
    }

    @Test
    public void testTransferMoneyToNegativeError() throws Exception {
        Account account1 = new Account();
        account1.setName("Alice");
        Account accountFromServer1 = HttpTestUtils.createAccount(account1, URL_MAIN, accountSerializer);

        Account account2 = new Account();
        account2.setName("Bob");
        Account accountFromServer2 = HttpTestUtils.createAccount(account2, URL_MAIN, accountSerializer);

        HttpTestUtils.updateBalance(5000, URL_MAIN, accountFromServer2.getId());
        HttpResponse<String> result = HttpTestUtils.transferMoney(accountFromServer1.getId(),
                accountFromServer2.getId(), 1000.,
                URL_MAIN);
        assertEquals(422, result.getStatus());
    }

    @Test
    public void testTransferMoneyToSameAccountError() throws Exception {
        Account account1 = new Account();
        account1.setName("Alice");
        Account accountFromServer = HttpTestUtils.createAccount(account1, URL_MAIN, accountSerializer);
        HttpTestUtils.updateBalance(5000, URL_MAIN, accountFromServer.getId());

        HttpResponse<String> result = HttpTestUtils.transferMoney(accountFromServer.getId(),
                accountFromServer.getId(), 1000.,
                URL_MAIN);
        assertEquals(422, result.getStatus());
    }

    @Test
    public void deleteAccountSucess() throws Exception{
        Account account = new Account();
        account.setName("Alice");
        Account accountFromServer = HttpTestUtils.createAccount(account, URL_MAIN, accountSerializer);
        HttpResponse<String> result = HttpTestUtils.deleteAccount(accountFromServer.getId(),
                URL_MAIN);
        assertEquals(200, result.getStatus());
    }

    @Test
    public void deleteAccountError() throws Exception{
        Account account = new Account();
        account.setName("Alice");
        Account accountFromServer = HttpTestUtils.createAccount(account, URL_MAIN, accountSerializer);
        HttpTestUtils.updateBalance(100, URL_MAIN, accountFromServer.getId());
        HttpResponse<String> result = HttpTestUtils.deleteAccount(accountFromServer.getId(),
                URL_MAIN);
        assertEquals(422, result.getStatus());
    }

    @Test
    public void concurrencyTest() throws Exception {
        List<String> accounts = new ArrayList<>();
        double commonAmount = 0.;
        double amountForAccount = TRANSFER_COUNT;
        for (int i = 0; i < ACCOUNT_COUNT; i++) {
            commonAmount += amountForAccount;
            accounts.add(createAndChangeBalanceAccount(amountForAccount));
        }

        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        List<Future<HttpResponse<String>>> responses = random.ints(TRANSFER_COUNT, 0, 100)
                .boxed()
                .map(amount -> (Callable<HttpResponse<String>>) () -> {
                    int id = random.nextInt(accounts.size());
                    String source = accounts.get(id);
                    String destination = accounts.get((id + 1) % accounts.size());
                    return HttpTestUtils.transferMoney(source, destination, amount, URL_MAIN);
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
                        return HttpTestUtils.getAccount(id, URL_MAIN, accountSerializer).getBalance();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(resultAmount, BigDecimal.valueOf(commonAmount));
    }

    private String createAndChangeBalanceAccount(double amount) {
        try {
            Account account = new Account();
            account.setBalance(BigDecimal.ZERO);
            account.setName("Jack");
            Account acc1 = HttpTestUtils.createAccount(account, URL_MAIN, accountSerializer);
            HttpTestUtils.updateBalance(amount, URL_MAIN, acc1.getId());
            return acc1.getId();
        } catch (Exception e) {
            throw new RuntimeException("Incorrect creating/updating");
        }
    }

}
