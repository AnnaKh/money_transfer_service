package engine;

import httpserver.OperationResult;
import model.Account;
import model.AccountSerializer;
import org.junit.Assert;
import org.junit.Test;
import store.Store;
import java.math.BigDecimal;



import static org.mockito.Mockito.*;


public class AccountManagerTest {


    private AccountSerializer accountSerializer = new AccountSerializer();
    private Store store = mock(Store.class);
    private AccountManager accountManager = new AccountManager(store);


    @Test
    public void testTransferMoney() {
        String id = "1234";
        Account account = new Account();
        account.setName("Alice");
        account.setId(id);
        account.setBalance(new BigDecimal(100));

        String id1 = "5678";
        Account account1 = new Account();
        account1.setName("Bob");
        account1.setId(id);
        account1.setBalance(new BigDecimal(500));

        when(store.get(id)).thenReturn(account);
        when(store.get(id1)).thenReturn(account1);

        OperationResult operationResult = accountManager
                .transferMoney(id, id1, new BigDecimal(50));

        try {
            Assert.assertFalse(operationResult.isError());
            Assert.assertEquals(account1.getBalance().intValue(), 550);
            Assert.assertEquals(account.getBalance().intValue(), 50);
            verify(store).get(id);
            verify(store).get(id1);
            verify(store).put(id, account);
            verify(store).put(id1, account1);
            verifyNoMoreInteractions(store);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testGetAccount() {

        String id = "1234";
        Account account = new Account();
        account.setName("Bob");
        account.setId(id);

        when(store.get(id)).thenReturn(account);
        OperationResult operationResult = accountManager.getAccount(id);
        try {
            Account account1 = accountSerializer.deserialize(operationResult.getText());
            Assert.assertEquals(id, account1.getId());
            verify(store).get(id);
            verifyNoMoreInteractions(store);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testChangeBalanceToPlus() {
        String id = "1234";
        Account account = new Account();
        account.setName("Bob");
        account.setId(id);
        account.setBalance(BigDecimal.ZERO);

        when(store.get(id)).thenReturn(account);
        OperationResult operationResult = accountManager.changeBalance(id, BigDecimal.TEN);

        try {
            Assert.assertFalse(operationResult.isError());
            Assert.assertEquals(0, account.getBalance().compareTo(new BigDecimal(10)));
        } catch (Exception e) {
            Assert.fail();
        }
        verify(store).get(id);
        verify(store).put(id, account);
        verifyNoMoreInteractions(store);
    }


    @Test
    public void testChangeBalanceToMinusFail() {
        String id = "1234";
        Account account = new Account();
        account.setName("Bob");
        account.setId(id);
        account.setBalance(BigDecimal.ZERO);

        when(store.get(id)).thenReturn(account);
        OperationResult operationResult =  accountManager.changeBalance(id, new BigDecimal(-10));

        try {
            Assert.assertTrue(operationResult.isError());
            Assert.assertEquals(0, account.getBalance().intValue());
        } catch (Exception e) {
            Assert.fail();
        }
        verify(store).get(id);
        verifyNoMoreInteractions(store);
    }


    @Test
    public void testAddAccount() {
        Account account = new Account();
        account.setName("Hunt");
        OperationResult operationResult =  accountManager.addAccount(account);
        try {
            Account acc = accountSerializer.deserialize(operationResult.getText());
            Assert.assertNotNull(acc.getId());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testDeleteNoMoneyAccountSuccess() {
        String accountId = "11234";
        Account account = new Account();
        account.setId(accountId);
        account.setBalance(BigDecimal.ZERO);
        when(store.get(accountId)).thenReturn(account);
        OperationResult operationResult = accountManager.deleteAccount(accountId);
        try {
            Assert.assertFalse(operationResult.isError());
            verify(store).get(accountId);
            verify(store).delete(accountId);
            verifyNoMoreInteractions(store);
        } catch (Exception e) {
            Assert.fail();
        }
    }


    @Test
    public void testDeleteWithMoneyAccountFail() {
        String accountId = "11234";
        Account account = new Account();
        account.setId(accountId);
        account.setBalance(BigDecimal.ONE);
        when(store.get(accountId)).thenReturn(account);
        OperationResult operationResult =  accountManager.deleteAccount(accountId);
        try {
            Assert.assertTrue(operationResult.isError());
            verify(store).get(accountId);
            verifyNoMoreInteractions(store);
        } catch (Exception e) {
            Assert.fail();
        }
    }
}
