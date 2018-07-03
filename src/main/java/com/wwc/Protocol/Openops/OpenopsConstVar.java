package com.wwc.Protocol.Openops;

public interface OpenopsConstVar {
    int TAG_LEN = 16;
    int LENGTH_FIELD_LEN = 2;
    int AUTH_HEADER = LENGTH_FIELD_LEN + TAG_LEN ;

    int ATYP_IPV4 = 0x01;
    int ATYP_DOMAIN = 0x03;

    int ATYP_IPV6 = 0x04;
}
