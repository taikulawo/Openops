package com.wwc.Dispatch.Factory;

import com.wwc.Main;
import com.wwc.Protocol.Inbound;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.wwc.Utils.Common.getNow;

import java.util.HashMap;


/**
 * InboundManager会记录所有创建的Inbound，而具体的Inbound创建则交由InboundFactory
 *
 * 由于Inbound最终创建由Inbound#getInstance()来负责，Inbound编写者必须自己维护所有创建的Inbound，
 * InboundManager不再维护,原本打算删了这个class，想想还是算了，如果想要添加v2ray那种的RPC调用，这个还是有用的
 */
public class InboundManager {

    private InboundFactory factory = new InboundFactory();


    public InboundManager(){ }

    public Inbound create(String name)
            throws Exception {

        Inbound in = factory.getInstance(name);
        return in;
    }

}
