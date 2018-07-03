package com.wwc.Protocol;

import io.vertx.core.net.NetSocket;

/**
 *对于实现Inbound的子类来说，由于Outbound#process是一个接口，
 * Processable<Mail,Buffer> p ;
 * void redirect(Mail mail, Processable p){
 *     this.p = p ;
 *
 * }
 *
 * void write(){
 *     p.process(mail,data);
 * }
 *
 * 这样就可以改变Inbound的写入方向了，那么Inbound接口的默认写入应该是Outbound#process.
 */
public interface Inbound extends IBound{

    void process(NetSocket socket);

    default Inbound getInstance(){
        Class<?> clazz = this.getClass();
        try {
            return (Inbound) clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

}
