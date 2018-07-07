package com.wwc;

import com.wwc.Crypto.IEncryptor;
import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static com.wwc.Utils.Common.getBytesArrayOfShort;
import static com.wwc.Utils.Common.getUnsignedshortFromBytesArray;

public class CommonTest {

    Logger log = LoggerFactory.getLogger(CommonTest.class);

    @Before
    public void prepare(){
        BasicConfigurator.configure();
    }

    @Test
    public void nonceIncrementTest() {
        byte[] nonce = new byte[12];
        int cnt = 20;
        while(cnt != 0){
            cnt --;
            IEncryptor.ivIncrement(nonce);
            log.debug(Arrays.toString(nonce));
        }
        log.error("",new Exception("exceptipn"));
    }

    @Test
    public void shortAndBytesArrayTest(){
        int s = 443;
        int d = getUnsignedshortFromBytesArray(getBytesArrayOfShort(s),0);
        assert s ==  d;
    }
}