eurpc
=====

an easy use rpc framework

# Tutorial

## Server
    // handler implemented a customized service interface
    Object handler; 
    
    // SimpleRpcServer could be replaced by NettyRpcServer
    // SimpleSerializer could be replaced by ProtostuffSerializer(implemented ServerSerializer)
    // WARN: SimpleSerializer not compatitable with NettyRpcServer
    RpcServer server = new SimpleRpcServer(port, SimpleSerializer.getInstance(), new Object[]{handler}); 
    server.start();

## Client
    // SimpleRpcClient could be replaced by NettyRpcClient
    // WARN: SimpleSerializer not compatitable with NettyRpcClient
    RpcClient client = new SimpleRpcClient(host, port, SimpleSerializer.getInstance());
    ServiceInterface service = client.proxy(ServiceInterface.class);
    // service.doSomething(); ...
    client.close();
    
# Sample

	// Greeting.java, both server side and client side need this interface
    public interface Greeting {
    	void sayHello(String name);
    }
    
    // GreetingImpl.java, implemented in the server side
    public class GreetingImpl implement Greeting {
    	pubic void sayHello(String name) {
    		System.out.println(name);
    	}
    } 
    
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
* [eurpc-0.1.0.jar](https://github.com/downloads/hfdiao/eurpc/eurpc-0.1.0.jar)