package httpserver;

import engine.AccountManager;
import io.javalin.Context;
import model.Account;
import model.AccountSerializer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MoneyTransferServerRoutes {

    private static final Logger LOG = LoggerFactory.getLogger(MoneyTransferServerRoutes.class);

    private static final int HTTP_CODE_SUCCESS = 200;
    private static final int HTTP_CODE_BAD_REQUEST = 400;
    private static final int HTTP_CODE_UNPROCESSABLE_ENTITY = 422;
    private static final int HTTP_CODE_SERVER_ERROR = 500;

    private final AccountSerializer accountSerializer;
    private final AccountManager accountManager;
    private final ExecutorService executorService;

    public MoneyTransferServerRoutes(AccountManager accountManager, AccountSerializer accountSerializer, int numOfThreads) {
        this.accountManager = accountManager;
        this.accountSerializer = accountSerializer;
        executorService = Executors.newFixedThreadPool(numOfThreads,
                t -> new Thread(t, "account-actions-thread"));
        Runtime.getRuntime().addShutdownHook(new Thread(
                () -> executorService.shutdown()));
    }

    void get(Context ctx) {
        String accountId = getIdFromRequest(ctx.request());
        if (accountId == null) {
            sendValidationException(ctx, "Invalid get account request");
        } else {
            submitAction(ctx, () -> accountManager.getAccount(accountId));
        }
    }

    void add(Context ctx) {
        String accountsSer = ctx.request().getParameter("account");
        if (accountsSer == null) {
            sendValidationException(ctx, "Invalid add account request");
        } else {
            LOG.info("Add account request {}", accountsSer);
            Account account = accountSerializer.deserialize(accountsSer);
            submitAction(ctx, () -> accountManager.addAccount(account));
        }
    }

    void changeBalance(Context ctx) {
        HttpServletRequest request = ctx.request();
        String accountId = getIdFromRequest(request);
        BigDecimal sum = getAmountFromRequest(request);
        if (accountId == null || sum == null) {
            sendValidationException(ctx, "Invalid change balance request");
        } else {
            LOG.info("Change balance request id {}  sum {}", accountId, sum);
            submitAction(ctx, () -> accountManager.changeBalance(accountId, sum));
        }
    }

    void moneyTransfer(Context ctx) {
        HttpServletRequest request = ctx.request();
        String accountFrom = request.getParameter("from");
        String accountTo = request.getParameter("to");
        BigDecimal sumToTransfer = getAmountFromRequest(request);
        if (accountFrom == null || accountTo == null || sumToTransfer == null) {
            sendValidationException(ctx, "Invalid transfer money request");
        } else {
            LOG.info("Transfer money request from {} to {} sum {}", accountFrom, accountTo, sumToTransfer);
            submitAction(ctx, () -> accountManager.transferMoney(accountFrom, accountTo,
                    sumToTransfer));
        }
    }

    void delete(Context ctx) {
        HttpServletRequest request = ctx.request();
        String accountId = getIdFromRequest(request);
        if (accountId == null) {
            sendValidationException(ctx, "Invalid delete account request");
        } else {
            LOG.info("Delete account request : account id {}", accountId);
            submitAction(ctx, () -> accountManager.deleteAccount(accountId));
        }
    }

    @Nullable
    private BigDecimal getAmountFromRequest(HttpServletRequest request) {
        String str = request.getParameter("amount");
        if (str == null) {
            return null;
        }
        try {
            return new BigDecimal(str);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Schedules asynchronous execution of the request on executorService thread pool,
     * and supplies the HTTP server with a future via which it will figure out that the request is executed
     */
    private void submitAction(Context context, Callable<OperationResult> resultCallable) {
        CompletableFuture<String> future = new CompletableFuture<>();
        context.result(future);

        executorService.submit(() -> {
            try {
                OperationResult result = resultCallable.call();
                context.response().setStatus(result.isError() ? HTTP_CODE_UNPROCESSABLE_ENTITY : HTTP_CODE_SUCCESS);
                future.complete(result.getText());
            } catch (Throwable e) {
                LOG.error(e.getMessage(), e);
                context.response().setStatus(HTTP_CODE_SERVER_ERROR);
                future.completeExceptionally(e);
            }
        });
    }

    private String getIdFromRequest(HttpServletRequest request) {
        return request.getParameter("id");
    }

    private void sendValidationException(Context context, String errorMessage) {
        HttpServletResponse response = context.response();
        response.setStatus(HTTP_CODE_BAD_REQUEST);
        context.result(errorMessage);
        LOG.info(errorMessage);
    }

}
