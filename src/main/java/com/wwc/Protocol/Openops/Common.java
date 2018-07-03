package com.wwc.Protocol.Openops;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.SocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

import static com.wwc.Protocol.Openops.OpenopsConstVar.ATYP_DOMAIN;
import static com.wwc.Protocol.Openops.OpenopsConstVar.ATYP_IPV4;
import static com.wwc.Protocol.Openops.OpenopsConstVar.ATYP_IPV6;
import static com.wwc.Utils.Common.getBytesArrayFromBytesArray;
import static com.wwc.Utils.Common.getUnsignedshortFromBytesArray;


public interface Common {
    Logger log = LoggerFactory.getLogger(Common.class);

    static Object[] processATYP(Buffer data, int index){
        int atyp = data.getByte(index);
        String host = "";
        int port = 0;
        Integer finalPosition = 0 ;
        switch(atyp){
            case ATYP_IPV4:{
                try {
                    host = InetAddress.getByAddress(data.getBytes(index + 1, 5)).getHostAddress();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }

                port = data.getUnsignedShort(index + 5);
                finalPosition = index + 7;
                break;
            }
            case ATYP_DOMAIN:{
                int len = data.getByte(index +1 );
                host = new String(data.getBytes(index + 2, index + 2 + len),Charset.forName("UTF-8"));

                port = data.getUnsignedShort(index + 2 + len);
                finalPosition = index + 2 + len + 2;
                break;
            }
            case OpenopsConstVar.ATYP_IPV6:{
                try {
                    host = InetAddress.getByAddress(data.getBytes(index + 1, 17)).getHostAddress();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                finalPosition = index + 19;
                port = data.getUnsignedShort(17);
                break;
            }
            default:{
                log.debug("cannot parse ATYP , error ATYP: [{}]",atyp);
                return new Object[]{null,0};
            }
        }

        return new Object[]{SocketAddress.inetSocketAddress(port,host),finalPosition};
    }

    static Object[] processATYP(byte[] src, int index){
        int atyp = src[index];
        String host = null;
        int finalPosition = 0;
        int port = -1;
        switch(atyp){
            case ATYP_IPV4:{
                try {
                    host = InetAddress.getByAddress(getBytesArrayFromBytesArray(src,index +  1,4)).getHostAddress();
                    port = getUnsignedshortFromBytesArray(getBytesArrayFromBytesArray(src,index + 5,2),0);
                    finalPosition = index + 7;
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                break;

            }
            case ATYP_DOMAIN:{
                int hostLen = src[index + 1];
                host = new String(getBytesArrayFromBytesArray(src,index + 2,hostLen),Charset.forName("UTF-8"));
                port = getUnsignedshortFromBytesArray(getBytesArrayFromBytesArray(src,index+2+hostLen,2),0);
                finalPosition = index + 2 + hostLen + 2;
                break;
            }

            case ATYP_IPV6:{
                try {
                    host = InetAddress.getByAddress(getBytesArrayFromBytesArray(src,index+ 1 ,16)).getHostAddress();
                    port = getUnsignedshortFromBytesArray(getBytesArrayFromBytesArray(src,17,2),0);
                    finalPosition = index+ 19;
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                break;
            }

        }
        assert port > 0 && port < 65536:"bad port " + port;
        return new Object[]{SocketAddress.inetSocketAddress(port,host),finalPosition};
    }


}
