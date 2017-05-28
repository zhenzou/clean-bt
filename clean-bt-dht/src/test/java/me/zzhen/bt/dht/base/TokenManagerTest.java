package me.zzhen.bt.dht.base;

import me.zzhen.bt.dht.NodeId;
import me.zzhen.bt.dht.Token;
import me.zzhen.bt.dht.TokenManager;
import org.junit.Test;

import java.util.Optional;

/**
 * Project:CleanBT
 * Create Time: 17-1-5.
 * Description:
 *
 * @author zzhen zzzhen1994@gmail.com
 */
public class TokenManagerTest {


    @Test
    public void newToken() throws Exception {
        for (int i = 0; i < 100; i++) {
            TokenManager.newToken(NodeId.genRandomId(), "test_method");
        }
        Optional<Token> token = TokenManager.getToken(10);
        System.out.println(token);
    }

    @Test
    public void clearTokens() throws Exception {
        for (int i = 0; i < 1000000; i++) {
            TokenManager.newToken(NodeId.genRandomId(), "test_method");
        }
        Thread.sleep(14 * 1000);
        TokenManager.clearTokens();
        Optional<Token> token = TokenManager.getToken(10);
        System.out.println(token);
        System.out.println(TokenManager.size());
    }

    @Test
    public void getToken() throws Exception {

    }

}