package com.wwc.Openops;

import com.wwc.Protocol.Openops.Common;
import com.wwc.Protocol.Openops.OpenopsConstVar;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.SocketAddress;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static com.wwc.Utils.Common.shortToBytesArray;

public class OpenopsTest {

    @Test
    public void CommonTest() throws UnknownHostException {
        String ipv4Host = "192.0.0.1";
        String ipv6Host ="2404:6800:1001:1001:1001:1001:1001:1001";
        String domainHost = "www.google.com";

        byte[] dIpv4 = getAddrBytes(ipv4Host,OpenopsConstVar.ATYP_IPV4);

        Object[] osIpv4 = Common.processATYP(Buffer.buffer(dIpv4),0);
        Object[] osIpv4BytesArray = Common.processATYP(dIpv4,0);


        SocketAddress addrIpv4 = (SocketAddress) osIpv4[0];
        SocketAddress addrIpv4Array = (SocketAddress)osIpv4BytesArray[0];

        assert ipv4Host.equals(addrIpv4.host());
        assert 443 == addrIpv4.port();
        assert ipv4Host.equals(addrIpv4Array.host());
        assert 443 == addrIpv4Array.port();


        //ipv6 test
        byte[] dIpv6 = getAddrBytes(ipv6Host,OpenopsConstVar.ATYP_IPV6);

        Object[] osIpv6 = Common.processATYP(Buffer.buffer(dIpv6),0);
        Object[] osIpv6BytesArray = Common.processATYP(dIpv6,0);

        SocketAddress addrIpv6 = (SocketAddress) osIpv6[0];
        SocketAddress addrIpv6Array = (SocketAddress)osIpv4BytesArray[0];

        assert ipv6Host.equals(addrIpv6.host());
        assert 443 == addrIpv6.port();
        assert ipv4Host.equals(addrIpv6Array.host());
        assert 443 == addrIpv6Array.port();


        //domain test
        byte[] dDomain = getAddrBytes(domainHost,OpenopsConstVar.ATYP_DOMAIN);

        Object[] osDomain  = Common.processATYP(Buffer.buffer(dDomain),0);
        Object[] osDomainBytesArray = Common.processATYP(dDomain,0);

        SocketAddress addrDomain = (SocketAddress)osDomain[0];
        SocketAddress addrDomainArray = (SocketAddress)osDomainBytesArray[0];

        assert domainHost.equals(addrDomain.host());
        assert 443 == addrDomain.port();
        assert domainHost.equals(addrDomainArray.host());
        assert 443 == addrDomainArray.port();
    }

    private byte[] getAddrBytes(String dst,int atyp) throws UnknownHostException {
        byte[] d = null;
        ByteBuffer dd = null;
        switch(atyp){
            case OpenopsConstVar.ATYP_IPV4:{
                d = new byte[7];
                dd = ByteBuffer.wrap(d);
                dd.put((byte)atyp);
                InetAddress inetAddr = InetAddress.getByName(dst);
                dd.put(inetAddr.getAddress());
                break;
            }
            case OpenopsConstVar.ATYP_IPV6:{
                d = new byte[19];
                dd = ByteBuffer.wrap(d);
                dd.put((byte)atyp);
                InetAddress inetAddr = InetAddress.getByName(dst);
                dd.put(inetAddr.getAddress());
                break;
            }
            case OpenopsConstVar.ATYP_DOMAIN:{
                int len = dst.length();
                d = new byte[1 + 1 + len + 2];
                dd = ByteBuffer.wrap(d);
                dd.put((byte)atyp);
                dd.put((byte)len);
                dd.put(dst.getBytes(Charset.forName("UTF-8")));
                break;
            }

        }
        dd.put(shortToBytesArray((short)443));
        dd.flip();
        return d;
    }



}
