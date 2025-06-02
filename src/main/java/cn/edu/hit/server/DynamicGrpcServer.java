package cn.edu.hit.server;

import cn.edu.hit.common.ProtoFileManager;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import io.grpc.*;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ServerCalls;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 动态gRPC服务器，支持运行时注册和处理服务
 */
public class DynamicGrpcServer {
    private final Server server;
    private final ProtoFileManager protoManager;
    private final ServiceImplementation serviceImpl;

    public DynamicGrpcServer(int port) throws Exception {
        this.protoManager = new ProtoFileManager();
        this.serviceImpl = new ServiceImplementation();

        // 加载proto文件
        protoManager.loadProtoFiles("src/main/resources/proto");

        // 创建服务器
        this.server = ServerBuilder.forPort(port)
                .addService(new DynamicServiceHandler())
                .build();
    }

    /**
     * 启动服务器
     */
    public void start() throws IOException {
        server.start();
        System.out.println("Server started, listening on port " + server.getPort());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            try {
                DynamicGrpcServer.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("*** server shut down");
        }));
    }

    /**
     * 停止服务器
     */
    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * 等待服务器终止
     */
    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * 动态服务处理器
     */
    private class DynamicServiceHandler implements BindableService {

        @Override
        public ServerServiceDefinition bindService() {
            ServerServiceDefinition.Builder serviceBuilder =
                    ServerServiceDefinition.builder("user.UserService");

            // 注册UserService的方法
            registerUserServiceMethods(serviceBuilder);

            return serviceBuilder.build();
        }

        private void registerUserServiceMethods(ServerServiceDefinition.Builder serviceBuilder) {
            Descriptors.ServiceDescriptor userService =
                    protoManager.getServiceDescriptor("user.UserService");

            if (userService == null) {
                System.err.println("UserService not found!");
                return;
            }

            // 注册GetUser方法
            Descriptors.MethodDescriptor getUserMethod =
                    userService.findMethodByName("GetUser");
            if (getUserMethod != null) {
                MethodDescriptor<DynamicMessage, DynamicMessage> methodDescriptor =
                        createMethodDescriptor(userService, getUserMethod);

                serviceBuilder.addMethod(methodDescriptor,
                        ServerCalls.asyncUnaryCall(
                                serviceImpl::getUser
                        ));
            }

            // 注册CreateUser方法
            Descriptors.MethodDescriptor createUserMethod =
                    userService.findMethodByName("CreateUser");
            if (createUserMethod != null) {
                MethodDescriptor<DynamicMessage, DynamicMessage> methodDescriptor =
                        createMethodDescriptor(userService, createUserMethod);

                serviceBuilder.addMethod(methodDescriptor,
                        ServerCalls.asyncUnaryCall(
                                serviceImpl::createUser
                        ));
            }
        }

        private MethodDescriptor<DynamicMessage, DynamicMessage> createMethodDescriptor(
                Descriptors.ServiceDescriptor serviceDescriptor,
                Descriptors.MethodDescriptor methodDescriptor) {
            String fullMethodName = serviceDescriptor.getFullName() + "/" + methodDescriptor.getName();

            return MethodDescriptor.<DynamicMessage, DynamicMessage>newBuilder()
                    .setType(MethodDescriptor.MethodType.UNARY)
                    .setFullMethodName(fullMethodName)
                    .setRequestMarshaller(ProtoUtils.marshaller(
                            DynamicMessage.getDefaultInstance(methodDescriptor.getInputType())
                    ))
                    .setResponseMarshaller(ProtoUtils.marshaller(
                            DynamicMessage.getDefaultInstance(methodDescriptor.getOutputType())
                    ))
                    .build();
        }
    }

    /**
     * 主函数
     */
    public static void main(String[] args) throws Exception {
        int port = 50051;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        DynamicGrpcServer server = new DynamicGrpcServer(port);
        server.start();
        server.blockUntilShutdown();
    }
}