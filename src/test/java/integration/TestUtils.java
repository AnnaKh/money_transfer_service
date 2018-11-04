package integration;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import model.Account;
import model.AccountSerializer;

import static org.junit.Assert.assertEquals;


public class TestUtils {

    public static Account createAccount(Account account, String urlMain, AccountSerializer accountSerializer) throws Exception {
        String url = urlMain + "add";
        HttpResponse<String> response = Unirest.get(url)
                .queryString("account", accountSerializer.serialize(account))
                .asObject(String.class);
        assertEquals(200, response.getStatus());
        Account accountFromServer = accountSerializer.deserialize(response.getBody());
        return accountFromServer;
    }

    public static Account getAccount(String accountId, String urlMain, AccountSerializer accountSerializer) throws Exception {
        HttpResponse<String> response = Unirest.get(urlMain + "get")
                .queryString("id", accountId)
                .asObject(String.class);
        assertEquals(200, response.getStatus());
        return accountSerializer.deserialize(response.getBody());
    }

    public static HttpResponse<String> transferMoney(String source, String destination, double sum, String urlMain) throws Exception {
        String request = urlMain + "transferMoney";
        return Unirest.get(request)
                .queryString("from", source)
                .queryString("to", destination)
                .queryString("amount", sum)
                .asString();
    }

    public static HttpResponse<String> deleteAccount(String id, String urlMain) throws Exception {
        return Unirest.get(urlMain + "delete")
                .queryString("id", id)
                .asString();
    }


    public static HttpResponse<String> updateBalance(double amount, String urlMain, String accountId) throws Exception {
        String url = urlMain + "changeBalance";
        return Unirest.get(url)
                .queryString("id", accountId)
                .queryString("amount", amount)
                .asObject(String.class);
    }
}
