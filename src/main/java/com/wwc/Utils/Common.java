package com.wwc.Utils;

import io.vertx.core.net.SocketAddress;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;

public class Common {

    private static Logger log = LoggerFactory.getLogger(Common.class);
    private  static HashMap<String,Object> config = new HashMap<>();

    private static MessageDigest messageDigest = null;

    static{
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static HashMap<String,Object> getConfig(String path){
        File file = new File(path);
        try {
            String json = new String(Files.readAllBytes(file.toPath()), Charset.forName("UTF-8"));
            JSONObject jsonObject = new JSONObject(json);
            config = (HashMap)jsonObject.toMap();
            boolean isLocal = (Boolean)config.get("isLocal");
            if(isLocal){
                config.remove("server");
            }else{
                config.remove("client");
            }

            return config;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    public static long getNow(){
        return Instant.now().toEpochMilli() / 1000L;
    }

    /**
     * Big endian
     * @param s
     * @return
     */
    public static byte[] shortToBytesArray(short s){
        byte[] d = new byte[Short.BYTES];
        d[0] = (byte)(s >> 8);
        d[1] = (byte)s;
        return d;
    }

    public static byte[] intToBytesArray(int i){
        byte[] d = new byte[Integer.BYTES];
        d[0] = (byte)(i >> 24);
        d[1] = (byte)(i >> 16);
        d[2] = (byte)(i >> 8);
        d[3] = (byte)i;
        return d;
    }

    public static String byteArrayIPToString(byte[] array) throws UnknownHostException {
        int length = array.length;
        if(length == 4 || length == 16){
            throw new IllegalArgumentException("bad ip raw array, current length: [" + length + ']');
        }

        return InetAddress.getByAddress(array).getHostAddress();
    }

    public static int bytePortToUnsignedInt(byte[] p){
        return ((p[0] & 0xff ) << 8)
                +
                (p[1] & 0xff);
    }

    public static byte[] getMD5(String s,int len){

        byte[] dst = new byte[len];
        getMD5(s.getBytes(Charset.forName("UTF-8")),0,dst,0,len);
        return dst;
    }


    //SHA-256 hash is 256 bits
    public static void getMD5(byte[] src,int start,byte[] dst,int index,int len) {
        messageDigest.update(src,start,src.length - start);
        byte[] md5 ;
        md5 = messageDigest.digest();
        System.arraycopy(md5,0,dst,0,len);
    }



    public static byte[] getBytesArrayOfShort(int i){

        if(i > Short.MAX_VALUE * 2 + 2){
            throw new IllegalArgumentException("s must less than or equals Short MAX_VALUE!");
        }
        short s = (short)i;
        byte[] d = new byte[2];

        d[0] = (byte)(s >> 8);
        d[1] = (byte) s;
        return d;
    }

    /**
     * THis method is big endian
     * @param src
     * @param offset
     * @return integer     *
     */
    public static int getUnsignedshortFromBytesArray(byte[] src, int offset){
        return ((src[offset] & 0xff) << 8) + (src[offset + 1] & 0xff);
    }


    /**
     * 只判断是不是ip地址，不判断是否合规
     * @param host
     * @return
     */
    public static boolean isIp(String host){

        if(host.contains("::")){//ipv6
            return true;
        }
        String [] part = host.split(".");
        if(part.length != 4 ){
            return false;
        }
        for(String p : part){
            if(!p.chars().allMatch(Character::isDigit)){
                return false;
            }
        }
        return true;
    }

    /**
     * 只判断长度是否为4，false则为Ipv6
     * @param host
     * @return
     */
    public static boolean isIpv4(String host){
        return !host.contains("::");
    }

    public static byte[] getBytesArrayFromBytesArray(byte[] src, int offset, int len){
        byte[] d = new byte[len];

        System.arraycopy(src,offset,d,0,len);
        return d;
    }
}
