package engine;

import httpserver.OperationResult;
import org.junit.Test;

import java.util.concurrent.Callable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class LockManagerTest {

    LockManager lockManager = new LockManager();
    Callable<OperationResult> callable = mock(Callable.class);

    @Test
    public void executeOnTwoLocks() throws Exception {
        String id1 = "23";
        String id2 = "56";
        lockManager.executeOnTwoLocks(id1, id2, callable);
        verify(callable).call();
        verifyNoMoreInteractions(callable);
    }

    @Test
    public void executeOnOneLock() throws Exception {
        String id1 = "23";
        lockManager.executeOnOneLock(id1, callable);
        verify(callable).call();
        verifyNoMoreInteractions(callable);
    }
}