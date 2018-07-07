package com.wwc.Protocol.Openops;

import com.wwc.Crypto.IEncryptor;
import io.vertx.core.buffer.Buffer;

import java.util.ArrayList;

import static com.wwc.Protocol.Openops.OpenopsConstVar.CHUNK_LEN_MASK;
import static com.wwc.Protocol.Openops.OpenopsConstVar.LENGTH_FIELD_LEN;
import static com.wwc.Protocol.Openops.OpenopsConstVar.TAG_LEN;
import static com.wwc.Utils.Common.getBytesArrayOfShort;

public class ProtocolImpl {

    public static ArrayList<Buffer> firstEncrypt(Buffer data, byte[] header, IEncryptor encryptor){
        int len = data.length();
        int headerLen = header.length;
        int afterEncLen = LENGTH_FIELD_LEN + TAG_LEN * 2 + headerLen + len;

        int dataLenWithHeader = CHUNK_LEN_MASK - LENGTH_FIELD_LEN - TAG_LEN * 2 - header.length;
        ArrayList<Buffer> bufList = new ArrayList<>();

        if(afterEncLen > CHUNK_LEN_MASK){
            byte[] d = new byte[dataLenWithHeader + headerLen];
            data.getBytes(0,dataLenWithHeader,d,headerLen);
            System.arraycopy(header,0,d,0,headerLen);
            Buffer buf = doRunningEncrypt(d,encryptor);

            bufList.add(buf);
        }else{
            byte[] d = new byte[len + headerLen];
            data.getBytes(0,data.length(),d,headerLen);
            System.arraycopy(header,0,d,0,headerLen);
            Buffer buf = doRunningEncrypt(d,encryptor);
            bufList.add(buf);
            return bufList;
        }

        data = data.slice(dataLenWithHeader,data.length());
        bufList.addAll(runningEncrypt(data,encryptor));

        return bufList;
    }

    private static Buffer doRunningEncrypt(byte[] src, IEncryptor encryptor){
        int len = src.length ;
        byte[] encrypted = new byte[len + TAG_LEN * 2 + LENGTH_FIELD_LEN];
        encryptor.encrypt(getBytesArrayOfShort(len+ TAG_LEN),0,LENGTH_FIELD_LEN,encrypted,0);
        encryptor.incrementIv(true);
        encryptor.encrypt(src,0,src.length,encrypted,TAG_LEN + LENGTH_FIELD_LEN,0);
        encryptor.incrementIv(true);
        return Buffer.buffer(encrypted);

    }


    public static ArrayList<Buffer> runningEncrypt(Buffer data, IEncryptor encryptor){
        ArrayList<Buffer> list = new ArrayList<>();
        int dataLenNoHeader = CHUNK_LEN_MASK - TAG_LEN * 2 - LENGTH_FIELD_LEN;
        int available = data.length();
        while(available > 0){
            int chunk = Math.min(available,dataLenNoHeader);
            available -= chunk;
            Buffer buf = doRunningEncrypt(data.getBytes(0,chunk),encryptor);
            list.add(buf);
            data = data.slice(chunk <= data.length() ? chunk : 0,data.length());
        }

        return list;
    }


}
