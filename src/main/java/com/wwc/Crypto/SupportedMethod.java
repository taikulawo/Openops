package com.wwc.Crypto;

public class SupportedMethod {
    public static final int AES_GCM = 1;
    public int ivLen;
    public int keyLen;
    public int tagLen;
    public int type ;


    /**
     * from now, type always is AES_GCM
     * @param ivLen iv length
     * @param keyLen secretKey length
     * @param tagLen auth tag len
     * @param type what type encrypt type.
     */
    public SupportedMethod(int ivLen, int keyLen, int tagLen, int type){
        this.ivLen = ivLen;
        this.keyLen = keyLen;
        this.tagLen = tagLen;
        this.type = type;
    }

}
