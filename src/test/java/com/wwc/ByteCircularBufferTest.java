package com.wwc;

import com.wwc.Utils.LengthBaseParser;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.wwc.Utils.Common.*;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class ByteCircularBufferTest {
    private static Random random = new Random();
    private static Queue<byte[]> queue = new LinkedList<>();
    private static MessageDigest digest;

    {
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Test
    public  void test(){
        putTest(65535,2,2,0,0);
    }



    private static void putTest(int maxLength,
                         int lengthFieldOffset,
                         int lengthFieldLength,
                         int lengthAdjustment,
                         int initialToStrip){
        LengthBaseParser parser = new LengthBaseParser(maxLength,
                lengthFieldOffset,
                lengthFieldLength,
                lengthAdjustment,
                initialToStrip,
                ByteCircularBufferTest::handle);

        int count = 1000;
        while(count >= 0){
            count --;
            byte[] d = generateArray(lengthFieldLength,lengthFieldOffset);
            byte[] digested = digest.digest(d);
            queue.offer(digested);

            ByteBuffer needPut = ByteBuffer.wrap(d);
            int middle = random.nextInt(d.length ) + 1;
            needPut.limit(middle);
            parser.put(needPut);
            needPut.limit(needPut.capacity());
            parser.put(needPut);

        }
    }

    private static void handle(byte[] d){
        byte[] md5 = queue.poll();
        assertArrayEquals(md5,digest.digest(d));
    }

    private static byte[] generateArray(int lengthFieldLength,
                                 int lengthFieldOffset){
        byte[] beforeLength = new byte[lengthFieldOffset];
        random.nextBytes(beforeLength);
        int ranInt = 0;
        if(lengthFieldLength == 1){
            ranInt = random.nextInt(200) + 10;

        }else if(lengthFieldLength == 2){
            ranInt = random.nextInt(1000) + 20000;
        }else if(lengthFieldLength == 4){
            ranInt = random.nextInt(2000) + 30000;
        }

        byte[] payload = new byte[lengthFieldLength];
        random.nextBytes(payload);

        int total = lengthFieldOffset + ranInt + lengthFieldLength;
        byte[] returned = new byte[total];
        System.arraycopy(beforeLength,0,returned,0,beforeLength.length);
        byte[] len = null;
        if(lengthFieldLength == 1){
            len = new byte[1];
            len[0] = (byte)ranInt;

        }else if(lengthFieldLength == 2){
            len = new byte[2];
            len = shortToBytesArray((short)ranInt);
        }else if(lengthFieldLength == 3){
            len = new byte[4];
            len = intToBytesArray(ranInt);
        }
        System.arraycopy(len,0,returned,beforeLength.length,len.length);
        System.arraycopy(payload,0,returned,beforeLength.length + len.length,payload.length);
        return returned;
    }
}
