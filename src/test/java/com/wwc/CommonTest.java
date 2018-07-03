package com.wwc;

import org.junit.Test;

import java.util.Arrays;

import static com.wwc.Utils.Common.getBytesArrayOfShort;
import static com.wwc.Utils.Common.getUnsignedshortFromBytesArray;

public class CommonTest {

    @Test
    public void nonceIncrementTest() {
        byte[] nonce = new byte[8];
        int count = 100;
        while (count > 0) {
            count--;
            if (++nonce[7] == 0) if (++nonce[6] == 0)
                if (++nonce[5] == 0) if (++nonce[4] == 0)
                    if (++nonce[3] == 0) if (++nonce[2] == 0)
                        if (++nonce[1] == 0) if (++nonce[0] == 0) break;


            //   long value = 0;
            //   for(int i = 0 ; i < nonce.length ; ++i){
            //       value = (value << 8) + (nonce[i] & 0xff);
            //   }
            //   System.out.println(value);

            System.out.println(Arrays.toString(nonce));
        }
    }

    @Test
    public void shortAndBytesArrayTest(){
        int s = 443;
        int d = getUnsignedshortFromBytesArray(getBytesArrayOfShort(s),0);
        assert s ==  d;
    }
}