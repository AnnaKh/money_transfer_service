package engine;


import httpserver.OperationResult;
import model.Account;
import model.AccountSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import store.RocksDbStore;
import store.Store;

import java.math.BigDecimal;
import java.util.UUID;


/**
 * Implementation of all main methods (add, get, changeBalance, transferMoney, delete)
 */
public class AccountManager {

    private static final Logger LOG = LoggerFactory.getLogger(RocksDbStore.class);

    private final Store store;
    private final LockManager lockManager;
    private final AccountSerializer accountSerializer;

    public AccountManager(Store store) {
        this.store = store;
        this.lockManager = new LockManager();
        this.accountSerializer = new AccountSerializer();
    }

    public OperationResult transferMoney(String accountFromId, String accountToId,
                                         BigDecimal sum) {
        if (accountFromId.equals(accountToId)) {
            return OperationResult.error("Same source and destination account.");
        }
        return lockManager.executeOnTwoLocks(accountFromId, accountToId, () -> {
            Account accountFrom = store.get(accountFromId);
            Account accountTo = store.get(accountToId);
            if (accountFrom == null) {
                return OperationResult.error("Source account does not exist.");
            }
            if (accountTo == null) {
                return OperationResult.error("Destination account does not exist.");
            }
            if (accountFrom.getBalance().subtract(sum).compareTo(BigDecimal.ZERO) < 0) {
                return OperationResult.error("Can not withdraw to negative value.");
            }

            accountFrom.setBalance(accountFrom.getBalance().subtract(sum));
            accountTo.setBalance(accountTo.getBalance().add(sum));
            store.put(accountFromId, accountFrom);
            store.put(accountToId, accountTo);
            LOG.info("Transferring money {} from account {} to account {}.", sum, accountFromId, accountToId);
            return OperationResult.success();
        });
    }

    public OperationResult getAccount(String accountId) {
        return lockManager.executeOnOneLock(accountId, () -> {
            Account account = store.get(accountId);
            if (account == null) {
                return OperationResult.error("The account does not exist.");
            }
            LOG.info("Sending account {} to client.", accountId);
            return OperationResult.success(accountSerializer.serialize(account));
        });
    }


    public OperationResult changeBalance(String accountId, BigDecimal sum) {
        return lockManager.executeOnOneLock(accountId, () -> {
            Account account = store.get(accountId);
            BigDecimal balance = account.getBalance();
            if (balance.add(sum).compareTo(BigDecimal.ZERO) < 0) {
                return OperationResult.error("Can not withdraw to negative balance.");
            }
            account.setBalance(balance.add((sum)));
            store.put(accountId, account);
            LOG.info("Changing balance {} to account {}.", sum, accountId);
            return OperationResult.success();
        });
    }

    public OperationResult addAccount(Account account) {
        String accountId = UUID.randomUUID().toString();
        account.setId(accountId);
        account.setBalance(BigDecimal.ZERO);
        return lockManager.executeOnOneLock(accountId, () -> {
            store.put(accountId, account);
            LOG.info("Account was added {}.", account.getName());
            return OperationResult.success(accountSerializer.serialize(account));
        });
    }

    public OperationResult deleteAccount(String accountId) {
       return lockManager.executeOnOneLock(accountId, () -> {
            Account account = store.get(accountId);
            if (account == null) {
                return OperationResult.error("Account does not exist.");
            }
            if (!account.getBalance().equals(BigDecimal.ZERO)) {
                return OperationResult.error("Account balance is not 0.");
            }
            store.delete(accountId);
            LOG.info("Account {} was deleted.", accountId);
            return OperationResult.success();
        });
    }
}






