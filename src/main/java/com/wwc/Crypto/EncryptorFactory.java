package com.wwc.Crypto;

import com.wwc.Crypto.AES.AEAD.AESGCM;

import java.util.HashMap;
import java.util.Map;

public class EncryptorFactory {
    private static HashMap<String,Class> registeredMethods = new HashMap<>();

    private static final String AES_PACKAGE_NAME = "com.wwc.Crypto.AES";
    private EncryptorFactory(){
    }

    static{
        for(String method : AESGCM.supportedCiphers()){
            if(!registeredMethods.containsKey(method)){
                registeredMethods.put(method,AESGCM.class);
            }
        }
    }

    public static IEncryptor getEncryptor(String method, String passwd)
            throws Exception {

        String mthd;
        if(!registeredMethods.containsKey(method)){
            mthd = "aes-128-gcm";
        }else{
            mthd = method;
        }
        Class clazz = registeredMethods.getOrDefault(mthd,null);
        if(clazz == null){
            throw new Exception(String.format("Initialized failed when try to get %s",mthd));
        }
        return (IEncryptor) clazz.getConstructor(String.class,String.class).newInstance(mthd,passwd);

    }

    public static String dumpRegisteredMethod(){
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String,Class> entry : registeredMethods.entrySet()){
            sb.append(String.format("%s -> %s",entry.getKey(),entry.getValue().getName()));
        }
        return sb.toString();
    }
}
