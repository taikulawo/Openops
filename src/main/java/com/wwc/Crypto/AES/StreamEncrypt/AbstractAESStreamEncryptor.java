package com.wwc.Crypto.AES.StreamEncrypt;

import com.wwc.Crypto.IEncryptor;

public abstract class AbstractAESStreamEncryptor implements IEncryptor {
    private String method;
    private String passwd;
    public AbstractAESStreamEncryptor(String m, String p){
        this.method = m;
        this.passwd = p;
    }

    abstract protected void init();
    abstract int encryptorUpdate(byte[] src, int inputOffset, int inputLen, byte[] dst, int outputOffset);
    abstract int decryptorUpdate(byte[] src, int inputOffset, int inputLen, byte[] dst, int outputOffset);
}
