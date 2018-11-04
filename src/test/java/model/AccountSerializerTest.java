package model;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

public class AccountSerializerTest {

    AccountSerializer accountSerializer = new AccountSerializer();

    @Test
    public void deserialize() {
        Account account = new Account();
        account.setId("3847");
        account.setName("Ivan");
        account.setBalance(BigDecimal.ZERO);
        String json = accountSerializer.serialize(account);
        Account afterAcc = accountSerializer.deserialize(json);
        assertEquals(afterAcc.getBalance(), account.getBalance());
        assertEquals(afterAcc.getId(), account.getId());
        assertEquals(afterAcc.getName(), account.getName());
    }

    @Test
    public void serialize() {
        String json = "{\"id\":\"3847\",\"name\":\"Ivan\",\"balance\":0}";
        Account account = accountSerializer.deserialize(json);
        String jsonAfter = accountSerializer.serialize(account);
        assertEquals(json, jsonAfter);
    }
}