package store;

import model.Account;

/**
 * Database for keeping accounts
 */
public interface Store {

    void put(String accountId, Account account);
    Account get(String accountId);
    void delete(String accountId);

}
