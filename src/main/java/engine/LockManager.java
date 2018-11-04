package engine;


import httpserver.OperationResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * LockManager synchronizes access to the same accounts from multiple threads.
 * It maintains a map "account id -> lock for this account"
 * which guarantees that no more than one thread can access the same account simultaneously
 */
class LockManager {

    private final Map<String, Lock> accountIdLockMap = new HashMap<>();

    OperationResult executeOnTwoLocks(String id1, String id2, Callable<OperationResult> resultCallable) {
        // obtaining locks for both accounts
        Lock lock1 = getLock(id1);
        Lock lock2 = getLock(id2);

        // in order to prevent a deadlock, we always lock the locks in order ascending by account id
        int comp = id1.compareTo(id2);
        if (comp > 0) {
            Lock temp = lock1;
            lock1 = lock2;
            lock2 = temp;
        }

        // performing the operation synchronized by both locks
        OperationResult operationResult;
        synchronized (lock2) {
            synchronized (lock1) {
                try {
                    operationResult = resultCallable.call();
                } catch (Exception e) {
                    throw new RuntimeException();
                }
            }
        }

        // removing the locks (in case they are not in use by other threads)
        removeLock(id1);
        removeLock(id2);

        return operationResult;
    }


    OperationResult executeOnOneLock(String id, Callable<OperationResult> resultCallable) {
        // obtaining a lock for this account
        Lock lock = getLock(id);

        // performing an operation synchronized by this lock
        OperationResult operationResult;
        synchronized (lock) {
            try {
                operationResult = resultCallable.call();
            } catch (Exception e) {
                throw new RuntimeException();
            }
        }

        // removing the lock (in case it is not in use by other threads)
        removeLock(id);
        return operationResult;
    }

    /**
     * Creates a lock (or reuses the one created by another thread) for a given account id.
     * The method is synchronized(this).
     * @param accountId
     * @return
     */
    private synchronized Lock getLock(String accountId) {
        Lock lock = accountIdLockMap.get(accountId);
        if (lock == null) {
            lock = new Lock();
            accountIdLockMap.put(accountId, lock);
        }
        lock.incWaiters();
        return lock;
    }

    /**
     * Removes a lock (or decrements its counter in case it is used by another thread) for a given account id.
     * The method is synchronized(this).
     * @param accountId
     * @return
     */
    private synchronized void removeLock(String accountId) {
        Lock lock = accountIdLockMap.get(accountId);
        if (lock.numOfWaiters == 1) {
            accountIdLockMap.remove(accountId);
        } else {
            lock.decWaiters();
        }
    }

    private static class Lock {
        int numOfWaiters;

        void incWaiters() {
            numOfWaiters++;
        }

        void decWaiters() {
            numOfWaiters--;
        }
    }

}
