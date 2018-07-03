package com.wwc.Crypto;

import java.util.HashMap;


//全部的加密IV不在单独进行传递，iv从0000 bytes array开始，然后每次的加密解密全部自加1，
//完全仿照Shadowsocks的加密方式，
//这样由于数据包在一次传递过程中需要进行一次ivlen和tcp payload的加密，所以每一次完整的
//加密方式iv一共增加两次。

public abstract class AbstractEncryptor implements IEncryptor {

    protected static HashMap<String,SupportedMethod> supportedMethods = new HashMap<>();

    protected String method;
    protected String passwd;

    public AbstractEncryptor(String method,String passwd){
        this.method = method;
        this.passwd = passwd;
    }

}
