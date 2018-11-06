package model;

import com.google.gson.Gson;

public class AccountSerializer {

    private final Gson gson = new Gson();

    public Account deserialize(String json){
        return gson.fromJson(json, Account.class);
    }

    public String serialize(Account account){
        return gson.toJson(account);
    }

}
