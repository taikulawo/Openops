package com.wwc.Utils;

@FunctionalInterface
public interface Handler<E> {
    void handle(E e);
}
