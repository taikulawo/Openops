package com.wwc;

import com.wwc.Crypto.AES.AEAD.AESGCM;
import com.wwc.Crypto.IEncryptor;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertArrayEquals;

public class AESGCMTest {


    private IEncryptor encryptor ;
    private IEncryptor decryptor;
    private String passwd = "hello";
    private Random random;

    @Before
    public void prepare(){
        encryptor = new AESGCM("aes-128-gcm",passwd);
        decryptor = new AESGCM("aes-128-gcm",passwd);
        random = new Random();
    }

    @Test
    public void encDecTest() throws Exception {
        int count = 1000;

        while(count > 0 ){
            --count;

            int len = random.nextInt(9000) + 1000;
            byte[] data = new byte[len];
            random.nextBytes(data);
            byte[] encrypted = new byte[len + 16];

            encryptor.encrypt(data,0,data.length,encrypted,0);
            encryptor.incrementIv(true);

            byte[] decrypted = new byte[len];
            decryptor.decrypt(encrypted,0,encrypted.length,decrypted,0);
            decryptor.incrementIv(false);

            assertArrayEquals(data,decrypted);
        }
    }
}
