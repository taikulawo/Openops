package com.wwc.Crypto.AES.AEAD;

import com.wwc.Crypto.AbstractEncryptor;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public abstract class AbstractAEADEncryptor extends AbstractEncryptor {

    public AbstractAEADEncryptor(String method, String passwd) {
        super(method, passwd);
    }

}
