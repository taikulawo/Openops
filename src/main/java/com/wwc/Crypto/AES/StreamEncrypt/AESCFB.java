package com.wwc.Crypto.AES.StreamEncrypt;

import com.wwc.Crypto.AbstractEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;

public final class AESCFB extends AbstractAESStreamEncryptor {
    private Logger log = LoggerFactory.getLogger(AESCFB.class);

    private int ivLen;
    private int tagLen;
    private Cipher encryptor;
    private Cipher decryptor;

    public AESCFB(String method, String passwd) {
        super(method, passwd);
    }

    @Override
    protected void init(){
    }

    @Override
    int encryptorUpdate(byte[] src, int inputOffset, int inputLen, byte[] dst, int outputOffset) {
        return 0;
    }

    @Override
    int decryptorUpdate(byte[] src, int inputOffset, int inputLen, byte[] dst, int outputOffset) {
        return 0;
    }

    @Override
    public void encrypt(byte[] in, int start, int inLength, byte[] out, int index, Object... os) {

    }

    @Override
    public void decrypt(byte[] in, int start, int inLength, byte[] out, int index, Object... os) {

    }

    @Override
    public void encryptUpdate(byte[] in, int inputOffset, int inputLen) {

    }

    @Override
    public void decryptUpdate(byte[] in, int inputOffset, int inputLen) {

    }
}
