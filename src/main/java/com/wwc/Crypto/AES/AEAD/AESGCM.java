package com.wwc.Crypto.AES.AEAD;

import com.wwc.Crypto.IEncryptor;
import com.wwc.Crypto.SupportedMethod;
import com.wwc.Protocol.IBound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.wwc.Utils.Common.getMD5;

//encrypt 和 decrypt独立使用两个不同的IV，由于每个加密对应着一个解密，所以encIv与decIv值相同。

public class AESGCM extends AbstractAEADEncryptor {
    private Logger log = LoggerFactory.getLogger(AESGCM.class);
    public static final int IV_LEN_12 = 12;
//    public static final int IV_LEN_
    protected SecureRandom rng = new SecureRandom();

    protected byte[] passwdMD5 ;

    protected byte[] encIv = new byte[IV_LEN_12]; //这样iv直接初始化成 0 bytes array
    protected byte[] decIv = new byte[IV_LEN_12];

    protected int ivLen;
    protected int tagLen;
    protected int keyLen;

    protected Cipher encryptor;
    protected Cipher decryptor;

    protected SecretKey key;


    static {
        //supported Methods from now.
        supportedMethods.put("aes-128-gcm",new SupportedMethod(12,16,16,SupportedMethod.AES_GCM));
        supportedMethods.put("aes-192-gcm",new SupportedMethod(12,24,16,SupportedMethod.AES_GCM));
        supportedMethods.put("aes-256-gcm",new SupportedMethod(12,32,16,SupportedMethod.AES_GCM));
    }

    //AES key length
    public static final int AES_GCM_KEY_LEN_128 = 128;
    public static final int AES_GCM_KEY_LEN_192 = 192;
    public static final int AES_GCM_KEY_LEN_256 = 256;

    public AESGCM(String mthd, String passwd) {
        super(mthd, passwd);
        SupportedMethod method = supportedMethods.get(mthd);

        init();

        try {
            encryptor = Cipher.getInstance("AES/GCM/NoPadding");
            decryptor = Cipher.getInstance("AES/GCM/NoPadding");
        } catch (NoSuchAlgorithmException
                | NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }

    public static List<String> supportedCiphers(){
        return new ArrayList<>(supportedMethods.keySet());
    }


    /**
     * Object... os
     * os[0] random bytes array which will be used generate GCMParameterSpec
     * if you don't want use this class, then you must extends this class
     * @param in
     * @param start
     * @param inLength
     * @param out
     * @param index
     * @param os
     */
    @Override
    public void encrypt(byte[] in, int start, int inLength, byte[] out, int index, Object... os)
            throws Exception {

        try {
            encryptor.init(Cipher.ENCRYPT_MODE,key, new GCMParameterSpec(tagLen * 8 , encIv));
        } catch (InvalidKeyException
                | InvalidAlgorithmParameterException e) {
            try {
                encryptor.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,encIv));
            } catch (InvalidKeyException
                    | InvalidAlgorithmParameterException e1) {
                throw new Exception("In AESGCM#Encrypt, failed");
            }
        }
        try {
            encryptor.doFinal(in,start,inLength,out,index);
        } catch (ShortBufferException | IllegalBlockSizeException | BadPaddingException e) {
            throw new Exception("In AESGCM#Encrypt, failed");
        }


    }


    /**
     * AESGCM default implementation
     * @param in
     * @param start
     * @param inLength
     * @param out
     * @param index
     * @param os os[0] is iv, when in encrypt and decrypt, iv will be used to generate GCMParameterSpec
     */
    @Override
    public void decrypt(byte[] in, int start, int inLength, byte[] out, int index,Object... os) throws Exception {

        try {
            decryptor.init(Cipher.DECRYPT_MODE,key, new GCMParameterSpec(tagLen * 8 , decIv));
        } catch (InvalidKeyException
                | InvalidAlgorithmParameterException e) {
            try {
                decryptor.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,decIv));
            } catch (InvalidKeyException
                    | InvalidAlgorithmParameterException e1) {
                throw new Exception("In AESGCM#Decrypt, failed");
            }
        }
        try {
            decryptor.doFinal(in,start,inLength,out,index);
        } catch (ShortBufferException | IllegalBlockSizeException | BadPaddingException e) {
            throw new Exception("In AESGCM#Decrypt, failed");
        }
    }

    @Override
    public void encryptUpdate(byte[] in, int inputOffset, int inputLen) {
        encryptor.update(in,inputOffset,inputLen);
    }

    @Override
    public void decryptUpdate(byte[] in, int inputOffset, int inputLen) {
        decryptor.update(in,inputOffset,inputLen);
    }



    private void init(){
        SupportedMethod supported = supportedMethods.get(method);
        ivLen = supported.ivLen;
        tagLen = supported.tagLen;
        keyLen = supported.keyLen;
        passwdMD5 = getMD5(passwd,keyLen);
        this.key = new SecretKeySpec(getMD5(passwd,keyLen),"AES");
    }

    @Override
    public void encryptorUpdateAAD(byte[] src, int startIndex, int len) {
        encryptor.updateAAD(src,startIndex,len);
    }

    @Override
    public void decryptorUpdateAAD(byte[] src, int startIndex, int len) {
        decryptor.updateAAD(src, startIndex, len);
    }

    @Override
    public void incrementIv(boolean isEncryptor) {
        IEncryptor.ivIncrement(isEncryptor ? encIv : decIv );
    }
}
