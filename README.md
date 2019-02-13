仿照 [v2ray](https://v2ray.com) 自己设计的代理软件， 通过插件的形式通过运行时反射来加载继承了Inbound 和 Outbound 接口的类来处理流量

这是一个具有了相应思想的玩具

总体的配置文件

```
{
    "isLocal":"true | false",
    "inbounds":[
        {
            "tl":"tcp | udp",
            "tag":"your inbound tag name",
            "main":"main class path, package url. com.wwc.Protocol.Socks.InboundHandler",
            "name":"protocol name",
            "sendto":"which outbound use, use tag here",
            "port":6000
        }
    ],
    "outbounds":[
        {
            "tl":"tcp | udp",
            "tag":"your outbound tag name",
            "main":"main class path, package url. com.wwc.Protocol.Socks.OutboundHandler",
            "name":"protocol name"
        }
    ]
}
```

通过指定不同的Inbound和Outbound Class位置来运行时加载