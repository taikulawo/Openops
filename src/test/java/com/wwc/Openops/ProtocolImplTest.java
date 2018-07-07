package com.wwc.Openops;

import com.wwc.Crypto.EncryptorFactory;
import com.wwc.Crypto.IEncryptor;
import io.vertx.core.buffer.Buffer;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import static com.wwc.Protocol.Openops.OpenopsConstVar.LENGTH_FIELD_LEN;
import static com.wwc.Protocol.Openops.OpenopsConstVar.TAG_LEN;
import static com.wwc.Protocol.Openops.ProtocolImpl.firstEncrypt;
import static org.junit.Assert.assertArrayEquals;

public class ProtocolImplTest {

    private Random random ;
    private IEncryptor encryptor;
    private IEncryptor decryptor;


    @Before
    public void prepare(){
        random = new Random();
        try {
            encryptor = EncryptorFactory.getEncryptor("aes-128-gcm","hello");
            decryptor = EncryptorFactory.getEncryptor("aes-128-gcm","hello");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void protocolEncryptTest(){
        int count = 100;
        byte[] header = new byte[18];
        random.nextBytes(header);
        while(count > 0){
            count --;
            int ranInt = random.nextInt(5000) + 5000;
            byte[] veryBigRandomBytesArray = new byte[ranInt];
            random.nextBytes(veryBigRandomBytesArray);
            Buffer buf = Buffer.buffer(veryBigRandomBytesArray);

            ArrayList<Buffer> encrypted = firstEncrypt(buf,header,encryptor);

            ArrayList<byte[]> decrypted = new ArrayList<>();
            for(Buffer b : encrypted){
                int len = b.length();
                try{
                    byte[] dd = new byte[2];
                    decryptor.decrypt(b.getBytes(0,LENGTH_FIELD_LEN + TAG_LEN),0,LENGTH_FIELD_LEN + TAG_LEN,dd,0);

                    decryptor.incrementIv(false);
                    b = b.slice(LENGTH_FIELD_LEN + TAG_LEN,b.length());

                    byte[] decryptedBuf = new byte[b.length() - TAG_LEN];

                    decryptor.decrypt(b.getBytes(),0,b.length(),decryptedBuf,0);
                    decryptor.incrementIv(false);
                    decrypted.add(decryptedBuf);
                }catch(Throwable e ){
                    new Throwable(e).printStackTrace();

                }

            }

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            decrypted.forEach(d ->{
                try {
                    dataOutputStream.write(d);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            byte[] total = byteArrayOutputStream.toByteArray();
            try {
                byteArrayOutputStream.close();
                dataOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Buffer buff = Buffer.buffer(total);

            assertArrayEquals(header,buff.getBytes(0,18));
            buff = buff.slice(18,buff.length());
            assertArrayEquals(veryBigRandomBytesArray,buff.getBytes());

        }
    }
}
