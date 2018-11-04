package integration;

import com.mashape.unirest.http.HttpResponse;
import entrypoint.MoneyTransferFactory;
import model.Account;
import model.AccountSerializer;
import org.junit.*;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class HttpApiTest {

    private static final String URL_MAIN = "http://127.0.0.1:10001/accounts/";
    private static final MoneyTransferFactory moneyTransferFactory = new MoneyTransferFactory();

    private final AccountSerializer accountSerializer = new AccountSerializer();


    @BeforeClass
    public static void setUp() {
        moneyTransferFactory.launch("test_config.yaml");
    }


    @AfterClass
    public static void tearDown() {
        moneyTransferFactory.stop();
    }

    @Test
    public void testCreateAccount() throws Exception {
        Account account = new Account();
        account.setName("Mary Poppins");
        Account accountFromServer = TestUtils.createAccount(account, URL_MAIN, accountSerializer);
        assertEquals(accountFromServer.getName(), "Mary Poppins");
        assertEquals(accountFromServer.getBalance(), BigDecimal.ZERO);
        assertNotNull(accountFromServer.getId());
    }

    @Test
    public void testUpdateBalanceSuccess() throws Exception {
        Account account = new Account();
        account.setName("Mary Poppins");
        Account accountFromServer = TestUtils.createAccount(account, URL_MAIN, accountSerializer);
        HttpResponse<String> response = TestUtils.updateBalance(100, URL_MAIN, accountFromServer.getId());
        assertEquals(200, response.getStatus());

        Account updatedAccount = TestUtils.getAccount(accountFromServer.getId(), URL_MAIN, accountSerializer);
        assertEquals(0, new BigDecimal(100).compareTo(updatedAccount.getBalance()));
    }

    @Test
    public void testUpdateBalanceToNegativeFail() throws Exception {
        Account account = new Account();
        account.setName("Mary Poppins");
        Account accountFromServer = TestUtils.createAccount(account, URL_MAIN, accountSerializer);
        HttpResponse<String> response  = TestUtils.updateBalance(-100, URL_MAIN, accountFromServer.getId());
        assertEquals(422, response.getStatus());

        Account updatedAccount = TestUtils.getAccount(accountFromServer.getId(), URL_MAIN, accountSerializer);
        assertEquals(0, BigDecimal.ZERO.compareTo(updatedAccount.getBalance()));
    }

    @Test
    public void testTransferMoneySuccess() throws Exception {
        Account account1 = new Account();
        account1.setName("Ronaldo");
        Account accountFromServer1 = TestUtils.createAccount(account1, URL_MAIN, accountSerializer);

        Account account2 = new Account();
        account2.setName("Peres");
        Account accountFromServer2 = TestUtils.createAccount(account2, URL_MAIN, accountSerializer);

        TestUtils.updateBalance(5000, URL_MAIN, accountFromServer2.getId());


        HttpResponse<String> result = TestUtils.transferMoney(accountFromServer2.getId(),
                accountFromServer1.getId(), 1000.,
                URL_MAIN);
        assertEquals(200, result.getStatus());

        Account updatedAccountRonaldo = TestUtils.getAccount(accountFromServer1.getId(), URL_MAIN, accountSerializer);
        assertEquals(0, new BigDecimal(1000).compareTo(updatedAccountRonaldo.getBalance()));

        Account updatedAccountPeres = TestUtils.getAccount(accountFromServer2.getId(), URL_MAIN, accountSerializer);
        assertEquals(0, new BigDecimal(4000).compareTo(updatedAccountPeres.getBalance()));
    }


    @Test
    public void testTransferMoneyToNegativeError() throws Exception {
        Account account1 = new Account();
        account1.setName("Ronaldo");
        Account accountFromServer1 = TestUtils.createAccount(account1, URL_MAIN, accountSerializer);

        Account account2 = new Account();
        account2.setName("Peres");
        Account accountFromServer2 = TestUtils.createAccount(account2, URL_MAIN, accountSerializer);

        TestUtils.updateBalance(5000, URL_MAIN, accountFromServer2.getId());


        HttpResponse<String> result = TestUtils.transferMoney(accountFromServer1.getId(),
                accountFromServer2.getId(), 1000.,
                URL_MAIN);
        assertEquals(422, result.getStatus());
    }

    @Test
    public void testTransferMoneyToSameAccountError() throws Exception {
        Account account1 = new Account();
        account1.setName("Ronaldo");
        Account accountFromServer = TestUtils.createAccount(account1, URL_MAIN, accountSerializer);
        TestUtils.updateBalance(5000, URL_MAIN, accountFromServer.getId());

        HttpResponse<String> result = TestUtils.transferMoney(accountFromServer.getId(),
                accountFromServer.getId(), 1000.,
                URL_MAIN);
        assertEquals(422, result.getStatus());
    }

    @Test
    public void deleteAccountSucess() throws Exception{
        Account account = new Account();
        account.setName("Ronaldo");
        Account accountFromServer = TestUtils.createAccount(account, URL_MAIN, accountSerializer);
        HttpResponse<String> result = TestUtils.deleteAccount(accountFromServer.getId(),
                URL_MAIN);
        assertEquals(200, result.getStatus());
    }

    @Test
    public void deleteAccountError() throws Exception{
        Account account = new Account();
        account.setName("Ronaldo");
        Account accountFromServer = TestUtils.createAccount(account, URL_MAIN, accountSerializer);
        TestUtils.updateBalance(100, URL_MAIN, accountFromServer.getId());
        HttpResponse<String> result = TestUtils.deleteAccount(accountFromServer.getId(),
                URL_MAIN);
        assertEquals(422, result.getStatus());
    }


}
