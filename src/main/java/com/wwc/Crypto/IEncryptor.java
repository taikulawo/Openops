package com.wwc.Crypto;

import java.security.SecureRandom;

public interface IEncryptor {
    void encrypt(byte[] in, int start,int inLength,byte[] out, int index,Object... os)
            throws Exception;

    void decrypt(byte[] in, int start,int inLength,byte[] out, int index,Object... os)
            throws Exception;

    void encryptUpdate(byte[] in, int inputOffset, int inputLen);
    void decryptUpdate(byte[] in, int inputOffset, int inputLen);


    static void generateRandom(byte[] dst, int start, int len,SecureRandom rng){
        if(dst == null || rng == null){
            throw new NullPointerException("dst or rng cannot be null");
        }

        if(start < 0
                || len <= 0
                || start + len > dst.length){
            throw new IllegalArgumentException("recheck your arguments");
        }
        if(start ==0 && len == dst.length){
            rng.nextBytes(dst);
            return;
        }

        byte[] random = new byte[len];
        rng.nextBytes(random);
        System.arraycopy(random,0,dst,start,len);
    }

    /**
     * nonce 每一次自加1
     * @param nonce
     */
    static void ivIncrement(byte[] nonce) {
        int len = nonce.length;
        for (int i = len - 1 ; i >= 0 ; i --){
            if(++nonce[i] != 0){
                break;
            }
        }

//        if(++nonce[11] == 0) if( ++ nonce[10] == 0)
//            if(++nonce[9] == 0 ) if(++ nonce[8] == 0)
//                if(++ nonce[7] == 0) if(++ nonce[6] == 0)
//                    if(++ nonce[5] == 0) if(++ nonce[4] == 0)
//                        if(++ nonce[3] == 0) if(++ nonce[2] == 0)
//                            if(++ nonce[1] == 0) if(++ nonce[0] == 0);
    }

    default void encryptorUpdateAAD(byte[] src, int startIndex, int len){

    }
    default void decryptorUpdateAAD(byte[] src, int startIndex, int len){

    }

    default void incrementIv(boolean isEncryptor){

    }
}
