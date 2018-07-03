package com.wwc.Utils;

import java.nio.ByteBuffer;
/**
 * when use get(*), you must check whether Buffer is empty. use object.empty == true.
 * Otherwise a runtime exception will be thrown.
 */
public class ByteCircularBuffer {
    private static final int DEFAULT_CAPACITY = 65535;

    private int readIndex = 0;

    private int writeIndex = 0;

    private Handler<byte[]> handler;
    private int waittingLen = 0;

    public boolean full = false;
    public boolean empty = true;

    private int available = 0;
    private int capacity = DEFAULT_CAPACITY;

    private byte[] buffer;

    public ByteCircularBuffer(int s){
        this(s,null);
    }

    public ByteCircularBuffer(){
        this(DEFAULT_CAPACITY,null);

    }

    public ByteCircularBuffer(int s, Handler<byte[]> h){
        if(s < 0){
            throw new IllegalArgumentException("The buffer capacity must greater than zero or equal to zero.");
        }
        this.capacity = s;
        buffer = new byte[capacity];
        this.handler = h;
    }


    /**
     * clear buffer. This method just clear flag, don't modify byte in buffer.
     */
    private void clear(){
        full = false;
        empty = true;
        available = 0;
        readIndex = 0;
        writeIndex = 0;
    }

    /**
     * put byte array to buffer.
     * @param d source array, will copy byte from it and put to buffer.
     * @param arrayIndex start copy position. if arrayIndex is 1, This method will copy d[1] and put it to buffer.
     * @param count copy length.
     */
    public void put(byte[] d, int arrayIndex, int count){

        if(d == null){
            throw new NullPointerException("d cannot be null");
        }
        if(d.length == 0){
            return;
        }

        if(arrayIndex < 0){
            throw new IllegalArgumentException("arrayIndex must be a positive");
        }
        if(count< 0){
            throw new IllegalArgumentException("count must be a positive");
        }
        if(available + count > capacity){
            throw new ArrayIndexOutOfBoundsException("The buffer does not have sufficient capacity to put new items.");
        }
        if(d.length < arrayIndex + count){
            throw new IllegalArgumentException("d.length must greater than arrayIndex + count or equal to arrayIndex + count");
        }
        empty = false;

        int srcIndex = arrayIndex;
        int byteToProcess = d.length;
        while(byteToProcess > 0){
            int chunk = Math.min(capacity - writeIndex,byteToProcess);
            System.arraycopy(d,srcIndex,buffer,writeIndex,chunk);
            writeIndex = (writeIndex + chunk == capacity) ? 0 : writeIndex + chunk;
            available += chunk;
            byteToProcess -= chunk;
            srcIndex += chunk;
        }
        if(available == capacity){
            full = true;
        }

        if(handler != null){
            if(waittingLen == 0){
                return;
            }
            if(available >= waittingLen ){
                byte[] call = new byte[waittingLen];
                get(call);
                handler.handle(call);
                waittingLen = 0;
            }
        }
    }

    public void setWaittingLength(int l){
        this.waittingLen = l;
    }

    public void setHandler(Handler<byte[]> h){
        this.handler = h;
    }

    public void put(byte[] src){
        put(src,0,src.length);
    }

    /**
     * Put bytes to buffer from d. This method will not check hasArray(),
     * it will read all bytes from position to limit and transfer them to buffer.
     * @param d src ByteBuffer.
     */
    public void put(ByteBuffer d){
        int remaining = d.remaining();
        byte[] b = new byte[remaining];
        d.get(b);
        put(b,0,remaining);
    }

    /**
     * ALL get methods will modify readIndex
     * Get bytes array from buffer. You must check available using ByteCircularBuffer#available before called.
     * otherwise IllegalArgumentException will be thrown.
     * @param size
     * @return
     */
    public byte[] get(int size){
        if(size < 0){
            throw new IllegalArgumentException("size must a positive");
        }

        if(size > available){
            throw new IllegalArgumentException("size must less than or equal to available bytes in buffer");
        }
        byte[] d = new byte[size];
        get(d,0,size);
        return d;
    }

    public void get(byte[] d){
        get(d,0,d.length);
    }

    /**
     * Get available bytes in buffer. don't modify readIndex.
     * @return available bytes.
     */
    public int  getAvailableBytes(){
        return this.available;
    }

    /**
     * ALL get methods will modify readIndex
     * get one byte from buffer.
     * @return byte you want.
     */
    public byte get(){
        if(empty){
            throw new RuntimeException("buffer is empty");
        }
        byte b = buffer[readIndex];
        if(++readIndex == capacity){
            readIndex = 0;
        }
        available --;

        return b;
    }


    /**
     * ALL get methods will modify readIndex.
     * Get byte array from ByteCircularBuffer.
     * @param d dst array, bytes will copy to d.
     * @param arrayIndex the first byte which copy to. for example, if arrayIndex is 2,
     *                   the first byte in Buffer.readIndex will copy to index 2, and so on.
     * @param count how many bytes want to copy.
     */
    public void get(byte[] d ,int arrayIndex, int count){
        if(d == null){
            throw new NullPointerException("d cannot set to null");
        }

        if(arrayIndex < 0){
            throw new IllegalArgumentException("arrayIndex must a positive");
        }

        if(count < 0){
            throw new IllegalArgumentException("count must a positive");
        }
        if(count > available){
            throw new IllegalArgumentException("Ringbuffer contents insufficient for take/read operation");
        }

        if(d.length < arrayIndex + count){
            throw new IllegalArgumentException("Destination array too small for requested output");
        }

        int dstIndex = arrayIndex;
        int byteToCopied = count;
        while(byteToCopied > 0){
            int chunk = Math.min(capacity - readIndex,byteToCopied);
            System.arraycopy(buffer,readIndex,d,dstIndex,chunk);
            readIndex = (readIndex + chunk == capacity) ? 0 : readIndex + chunk;
            byteToCopied -= chunk;
            dstIndex += chunk;
            available -= chunk;
        }
        if(available == 0){
            empty = true;
        }
        full = false;
    }

    /**
     * skip some bytes. This method will modify readIndex.
     * @param length how many bytes want to skip.
     */
    public void skip(int length){
        if(length < 0){
            throw new IllegalArgumentException("length must be a positive");
        }

        if(length > available){
            throw new IllegalArgumentException("length must less than or equal to available bytes in buffer");
        }

        readIndex = (readIndex + length) % capacity;
        available -= length;
    }


    /**
     * ALL peek methods will never modify readIndex.
     * copy bytes from buffer to array, the first byte index is readIndex in buffer.
     * length is array.length.
     * @param array destination array which copy to.
     */
    public void peek(byte[] array){
        peek(array,0,array.length);
    }


    /**
     * copy bytes from buffer to array.
     * @param array destination array which copy to.
     * @param dstIndex dst index where first bytes put in array.
     * @param count copy length.
     */
    public void peek(byte[] array, int dstIndex,int count){
        copyTo(readIndex,array,dstIndex,count);
        return;
    }

    /**
     *
     * ALL peek methods will never modify readIndex.
     * copy bytes from buffer and return copied byte array.
     * @param count bytes length want to copy.
     * @return byte array.
     */
    public byte[] peek(int count){
        byte[] d = new byte[count];
        peek(d,0,count);
        return d;
    }


    /**
     * ALL peek methods will never modify readIndex.
     * relative to readIndex. for example, if relative is 0, the first index copy from buffer is readIndex,
     * if relative is 1, first index is readIndex + 1.
     * @param relative relative to readIndex.
     * @param array destination array which copy bytes to.
     * @param dstIndex dst index in array.
     * @param count copy length.
     */
    public void peek(int relative,byte[] array, int dstIndex, int count){
        copyTo(readIndex + relative,array,dstIndex,count);
        return;
    }

    /**
     * copy some bytes from buffer to dst array, This method don't modify buffer readIndex.
     * @param index start index in buffer.
     * @param dst The destination byte array.
     * @param dstIndex start index when copy to dst array.
     * @param count copy length.
     */
    private void copyTo(int index, byte[] dst, int dstIndex,int count){

        while(count > 0){
            int chunk = Math.min(capacity - index,count);
            System.arraycopy(buffer,index,dst,dstIndex,chunk);
            index = index + count == capacity ? 0 : index + count;
            count -= chunk;
            dstIndex += chunk;
        }
    }

    /**
     * Call before put new bytes into Buffer,
     * @param length put length.
     * @return true -> allow, false -> disallow.
     */
    public boolean allowPutBuffer(int length){
        return available >= length;
    }


}
