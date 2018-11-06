package httpserver;


import config.HttpSettings;
import io.javalin.Javalin;
import io.javalin.embeddedserver.jetty.EmbeddedJettyFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.javalin.ApiBuilder.*;


/**
 * Http server with routing for main operations
 */
public class MoneyTransferHttpServer {

    private static final int IDLE_TIMEOUT = 60_000;
    private static final Logger LOG = LoggerFactory.getLogger(MoneyTransferHttpServer.class);

    private final Javalin app;
    private final MoneyTransferServerRoutes moneyTransferServerRoutes;

    public MoneyTransferHttpServer(MoneyTransferServerRoutes moneyTransferServerRoutes,
                                   HttpSettings httpSettings,
                                   int numOfThreads) {

        this.moneyTransferServerRoutes = moneyTransferServerRoutes;
        QueuedThreadPool javalinThreadPool = new QueuedThreadPool(numOfThreads, 2, IDLE_TIMEOUT);
        app = Javalin.create()
                .port(httpSettings.port())
                .embeddedServer(new EmbeddedJettyFactory(() -> new Server(javalinThreadPool)));
        routes();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> app.stop()));
    }

    public void start() {
        app.start();
    }

    public void stop() {
        app.stop();
    }

    private void routes() {
        app.routes(() ->
                path("accounts", () -> {
                    get("changeBalance", ctx -> moneyTransferServerRoutes.changeBalance(ctx));
                    get("transferMoney", ctx -> moneyTransferServerRoutes.moneyTransfer(ctx));
                    get("delete", ctx -> moneyTransferServerRoutes.delete(ctx));
                    get("add", ctx -> moneyTransferServerRoutes.add(ctx));
                    get("get", ctx -> moneyTransferServerRoutes.get(ctx));
                })
        );
    }

}
