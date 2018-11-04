package entrypoint;

import config.ConfigKeeper;
import engine.AccountManager;
import httpserver.MoneyTransferHttpServer;
import httpserver.MoneyTransferServerRoutes;
import model.AccountSerializer;
import store.RocksDbStore;
import store.Store;

/**
 * Entry point with all main objects creation
 */
public class MoneyTransferFactory {

    private MoneyTransferHttpServer moneyTransferHttpServer;


    public void launch(String configName) {
        ConfigKeeper configKeeper = new ConfigKeeper(configName);
        AccountSerializer accountSerializer = new AccountSerializer();
        Store store = new RocksDbStore(accountSerializer, configKeeper.getStoreSettings());
        AccountManager accountManager = new AccountManager(store);
        int numOfThreads = getMaxThreads();
        MoneyTransferServerRoutes moneyTransferServerRoutes = new MoneyTransferServerRoutes(accountManager, accountSerializer, numOfThreads);
        moneyTransferHttpServer = new MoneyTransferHttpServer(moneyTransferServerRoutes, configKeeper.getHttpSettings(), numOfThreads);
        moneyTransferHttpServer.start();
    }
    public void stop() {
        if (moneyTransferHttpServer == null){
            return;
        }
        moneyTransferHttpServer.stop();
    }


    private static int getMaxThreads() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static void main(String[] args) {
        String filename = args.length == 0 ? "config.yaml" : args[0];
        MoneyTransferFactory  moneyTransferFactory = new MoneyTransferFactory();
        moneyTransferFactory.launch(filename);
    }
}
