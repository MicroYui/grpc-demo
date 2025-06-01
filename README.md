# gRPC动态调用Demo

本项目演示了如何在Java中实现gRPC动态调用，无需预生成stub代码。

## 项目特性

- **动态Proto加载**: 运行时编译和加载proto文件
- **无需代码生成**: 不依赖预生成的gRPC stub代码
- **JSON支持**: 支持JSON格式的请求和响应
- **交互式客户端**: 提供命令行交互界面
- **灵活扩展**: 可动态添加新的服务和方法

## 技术栈

- Java 8+
- gRPC
- Protocol Buffers
- Jackson (JSON处理)
- Maven

## 项目结构 
grpc-demo/\
├── src/main/\
│ ├── java/cn/edu/hit/\
│ │ ├── server/ # 服务器端代码\
│ │ ├── client/ # 客户端代码\
│ │ └── common/ # 公共工具类\
│ └── resources/proto/ # Proto定义文件\
├── pom.xml\
└── README.md


## 快速开始

### 1. 编译项目

```bash
mvn clean compile
```

### 2. 启动服务器

```bash
mvn exec:java -Dexec.mainClass="cn.edu.hit.server.DynamicGrpcServer"
```

服务器默认在50051端口启动。

### 3. 启动客户端

```bash
mvn exec:java -Dexec.mainClass="cn.edu.hit.client.GrpcClientManager"
```

### 4. 使用客户端

客户端启动后，你可以使用以下命令：

- `help` - 显示帮助信息
- `list` - 列出所有可用的服务和方法
- `call <service>.<method>` - 调用指定的gRPC方法
- `quit` - 退出客户端

#### 示例调用

1. 获取用户信息：
grpc> call user.UserService.GetUser
request> {"user_id": 1}

2. 创建新用户：
grpc> call user.UserService.CreateUser
request> {"name": "John", "email": "john@example.com", "age": 28}

## 核心组件说明

### ProtoFileManager
负责动态编译和加载proto文件，生成服务和方法的描述符。

### DynamicGrpcClient
动态gRPC客户端，支持运行时调用任意gRPC服务，支持JSON格式的请求和响应。

### DynamicGrpcServer
动态gRPC服务器，支持运行时注册和处理服务方法。

### MethodDescriptorHelper
工具类，用于创建gRPC方法描述符。

## 扩展说明

### 添加新服务

1. 在`src/main/resources/proto/`目录下添加新的proto文件
2. 在`ServiceImplementation`类中添加对应的方法实现
3. 在`DynamicGrpcServer`中注册新的服务方法

### 支持流式调用

当前demo主要演示unary调用，可以扩展支持客户端流、服务端流和双向流调用。

## 注意事项

- proto文件修改后需要重启服务器
- 确保proto文件语法正确
- JSON请求格式需要与proto定义匹配
- 当前实现为演示目的，生产环境使用时需要添加错误处理和安全机制

## 许可证

MIT License