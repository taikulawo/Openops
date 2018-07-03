package com.wwc.Utils;

import io.vertx.core.Vertx;

public class Timer {
    private Vertx vertx;
    public Timer(Vertx v){
        this.vertx = v;

    }

    public void setOneShotTimer(long delay, io.vertx.core.Handler<Long> handler){
        vertx.setTimer(delay,handler);
    }

}
