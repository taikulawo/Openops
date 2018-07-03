package com.wwc.Socket;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;

public interface ISocket<T> {

    /**
     *
     * @param data want to write to socket.
     * @return false if write queue full, true if write successfully.
     * But not worry, data will write to socket when have chance.
     */
    boolean write(T data);


    /**
     * called when socket occur exception
     * @param handler whill be called with exception.
     */
    void exceptionHandler(Handler<Throwable> handler);

    //void asyncDrainHandler(Handler<Void> handler);


    /**
     * will be called after socket has been closed
     */
    void closeHandler(Handler<Void> handler);
    void close();


    void end();

    void endHandler(Handler<Void> handler);

    void handler(Handler<Buffer> handler);
}
