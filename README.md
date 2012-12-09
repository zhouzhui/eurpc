eurpc
=====

an easy use rpc framework

# Tutorial

## Server
    // handler implemented a customized service interface
    Object handler; 
    
    // SimpleRpcServer could be replaced by NettyRpcServer(SimpleSerializer not compatitable for NettyRpcServer)
    // SimpleSerializer could be replaced by ProtostuffSerializer(implemented ServerSerializer)
    RpcServer server = new SimpleRpcServer(port, SimpleSerializer.getInstance(), new Object[]{handler}); 
    server.start();

## Client
    // SimpleRpcClient could be replaced by NettyRpcClient(SimpleSerializer not compatitable for NettyRpcClient)
    RpcClient client = new SimpleRpcClient(host, port, SimpleSerializer.getInstance());
    ServiceInterface service = client.proxy(ServiceInterface.class);
    // service.doSomething(); ...
    client.close();
    
# Dependency
* SimpleRpcServer: none  
* SimpleRpcClient: none  
* SimpleSerializer: none  
* NettyRpcServer: netty(3.2.1.Final)  
* NettyRpcClient: netty(3.2.1.Final)  
* ProtostuffSerializer: protostuff-core(1.0.7), protostuff-runtime(1.0.7)  
* ProtobufSerializer: protostuff-core(1.0.7), protostuff-runtime(1.0.7)  
* GraphSerializer: protostuff-core(1.0.7), protostuff-runtime(1.0.7)  

# Downloads
* [eurpc-0.1.0.jar] (https://github.com/downloads/hfdiao/eurpc/eurpc-0.1.0.jar)