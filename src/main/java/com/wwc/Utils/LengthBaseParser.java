package com.wwc.Utils;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 *  a parser like netty length field based frame decoder
 *  use ByteCircularBuffer to cache all data receive from remote
 */


//当object创建之后不允许再修改lengthFieldOffset等字段
    //This Class is NOT Thread　safe.
//默认情况下length field排除自身的长度，如果length field表示了自身的长度，最后需要
    //lengthAdjustment进行调整。
public class LengthBaseParser {
    private static Logger log = LoggerFactory.getLogger(LengthBaseParser.class);
    private static final int DEFAULT_MAX_FRAME_LENGTH = 65535;

    //need is what we need data's length from buffer.readIndex,
    // if initialBytesToStrip is special, we must remove some bytes before we callback.
    private int need = 0;

    private Handler<byte[]> handler;

    //buffer size
    private  int maxFrameLength = DEFAULT_MAX_FRAME_LENGTH;

    private Handler<byte[]> callBackHandler ;

    private ByteCircularBuffer buffer;

    //
    private final int lengthFieldOffset;

    //
    private final int lengthFieldLength;

    //
    private final int lengthAdjustment;

    //strip length when get need bytes
    private final int initialBytesToStrip;

    //
    private int lengthFieldEndOffset ;

    private boolean waittingMoreData = false;

    public LengthBaseParser(int maxLength,
                      int lengthFieldOffset,
                      int lengthFieldLength,
                      int lengthAdjustment,
                      int initialBytesToStrip,
                      Handler<byte[]> h){

        this.maxFrameLength = maxLength;
        this.lengthFieldOffset = lengthFieldOffset;
        this.lengthFieldLength = lengthFieldLength;
        this.lengthAdjustment = lengthAdjustment;
        this.initialBytesToStrip = initialBytesToStrip;
        buffer = new ByteCircularBuffer(maxLength);
        lengthFieldEndOffset = lengthFieldOffset + lengthFieldLength;
        this.handler = h;
    }


    public void put(byte[] src,int arrayIndex, int count){
        if(src == null){
            throw new NullPointerException("src cannot set to null");
        }
        if(arrayIndex < 0){
            throw new IllegalArgumentException("arrayIndex must be a positive");
        }

        if(count < 0){
            throw new IllegalArgumentException("count must be a positive");
        }

        if(src.length < arrayIndex + count){
            throw new IllegalArgumentException("The buffer does not have sufficient capacity to put new items");
        }

        byte[] needPut = new byte[count];
        System.arraycopy(src,arrayIndex,needPut,0,count);
        put(needPut);
    }

    public void put(byte[] src){
        buffer.put(src);

        checkNeedBytes();
    }

    public void put(ByteBuffer src){
        byte[] s = new byte[src.remaining()];
        src.get(s);
        put(s);
    }


    private void checkNeedBytes(){
        int available = buffer.getAvailableBytes();

        log.debug("available size: [{}] in buffer",available);

        if(!waittingMoreData){
            if(available >= lengthFieldEndOffset){
                byte[] d = new byte[lengthFieldEndOffset];
                buffer.peek(d);
                getPackageLength(d);

                log.debug("needed length: [{}]",need);

                waittingMoreData = true;
            }else{
                return;
            }
        }else{
            if(available >= need){
                log.debug("handle completed buffer, need: [{}], initialBytesToStrip: [{}]",need,initialBytesToStrip);
                buffer.skip(initialBytesToStrip);
                byte[] d = new byte[need-initialBytesToStrip];
                buffer.get(d);
                waittingMoreData = false;
                handler.handle(d);
            }else{
                return;
            }
        }
        checkNeedBytes();
    }


    private void getPackageLength(byte[] d){
        need = getFrameLengthAccordingLengthField(d,lengthFieldOffset,lengthFieldLength)
                + lengthAdjustment + lengthFieldEndOffset;
    }

    /**
     * Now, we only support 1, 2, 4 bytes of lengthFieldLength.
     * when lengthFieldLength is 1 or 2, we think it's unsigned.
     * if lengthFieldLength is 4 bytes, we think it's signed integer.
     * @param d array, we will get length from it.
     * @param offset start position in d where we calculate length.
     * @param lengthFieldLength lengthFieldLength.
     * @return length according to lengthFieldLength. if we want to get the whole message length,
     * we should care about lengthFieldOffset and lengthAdjustment.
     */
    private int getFrameLengthAccordingLengthField(byte[] d,int offset,int lengthFieldLength){
        ByteBuffer buf = ByteBuffer.wrap(d);
        int frameLength = 0;
        switch(lengthFieldLength){
            case 1:{
                frameLength = buf.get(offset) & 0xff;
                break;
            }
            case 2:{
                frameLength = buf.getShort(offset) & 0xffff;
                break;
            }
            case 3:{
                frameLength = buf.getInt(offset);
                break;
            }
            default:{
                throw new IllegalArgumentException("we only support 1, 2, 4 bytes of lengthFieldLength");
            }
        }
        return frameLength;
    }
}
