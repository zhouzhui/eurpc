eurpc
=====

an easy use rpc framework

# Tutorial

## Start a server

	// handler implemented a customized service interface
    CustomizedServiceInterface handler; 
    
    /*
     * 1. BIORpcServer could be replaced by NettyRpcServer
     * 2. JDKObjectSerializer could be replaced by ProtostuffSerializer(or other serializers implemented ServerSerializer)
     */
    RpcServer server = new BIORpcServer(port, JDKObjectSerializer.getInstance(), new Object[]{handler}); 
    server.start();

## Invoke server side methods

    /*
     * 1. BIORpcConnection could be replaced by NettyRpcConnection
     * 2. use RpcConnectionFactory to construct SimpleRpcClient
     * RpcConnectionFactory factory = new BIORpcConnectionFactory(host, port, JDKObjectSerializer.getInstance());
     * factory = new PoolableRpcConnectionFactory(factory);
     * RpcClient client = new SimpleRpcClient(factory);
     */
    RpcConnection conn = new BIORpcConnection(host, port, JDKObjectSerializer.getInstance());
    RpcClient client = new SimpleRpcClient(conn);
    
    CustomizedServiceInterface service = client.proxy(CustomizedServiceInterface.class);
    // service.doSomething(); ...
    client.destroy();
    
# Build from code

## Pre-requirement

* JDK6+
* Maven 2

## Build

	git clone https://github.com/hfdiao/eurpc.git eurpc
	cd eurpc
	mvn package
    
# Dependency

* BIORpcServer: none
* BIORpcConnection: none  
* BIORpcConnectionFactory: none  
* JDKObjectSerializer: none  
* NettyRpcServer: netty(3.2.1.Final)  
* NettyRpcConnection: netty(3.2.1.Final)  
* NettyRpcConnectionFactory: netty(3.2.1.Final)  
* PoolableRpcConnectionFactory: commons-pool(1.6)
* ProtostuffSerializer: protostuff-core(1.0.7), protostuff-runtime(1.0.7)  
* ProtobufSerializer: protostuff-core(1.0.7), protostuff-runtime(1.0.7)  
* GraphSerializer: protostuff-core(1.0.7), protostuff-runtime(1.0.7)  

