package com.wwc.Protocol;

import com.wwc.Utils.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface IBound {
    Logger log = LoggerFactory.getLogger(IBound.class);
    int END_ACTION = 0;
    int EXCEPTION_ACTION = 1;
    int CLOSE_ACTION = 2;

    //进行与自身close有关的操作
    void close();

    default void tell(int action, Object o){
        switch(action){
            case EXCEPTION_ACTION:{
                log.debug("[{}]",(Throwable)o);
            }
            case END_ACTION:{

            }
            case CLOSE_ACTION:{

            }
        }
        close();
    }
}
