namespace java com.github.drpc.thrift

struct HelloRequest {
	1: string name
}

struct HelloResponse {
	1: string message
}

service GreeterService {
    HelloResponse sayHello(1: HelloRequest req)
}
