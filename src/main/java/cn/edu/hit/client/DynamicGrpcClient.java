package cn.edu.hit.client;

import cn.edu.hit.common.MethodDescriptorHelper;
import cn.edu.hit.common.ProtoFileManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.grpc.stub.ClientCalls;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 动态gRPC客户端，支持运行时调用任意gRPC服务
 */
public class DynamicGrpcClient {
    private final ManagedChannel channel;
    private final ProtoFileManager protoManager;
    private final ObjectMapper objectMapper;
    private final JsonFormat.Parser jsonParser;
    private final JsonFormat.Printer jsonPrinter;

    public DynamicGrpcClient(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        this.protoManager = new ProtoFileManager();
        this.objectMapper = new ObjectMapper();
        this.jsonParser = JsonFormat.parser().ignoringUnknownFields();
        this.jsonPrinter = JsonFormat.printer();
    }

    /**
     * 加载proto文件
     */
    public void loadProtoFiles(String protoDir) throws Exception {
        protoManager.loadProtoFiles(protoDir);
    }

    /**
     * 调用gRPC方法（使用JSON格式的请求和响应）
     */
    public String callMethod(String serviceName, String methodName, String requestJson)
            throws Exception {

        System.out.println("Debug: Calling service=" + serviceName + ", method=" + methodName);

        // 获取服务描述符
        Descriptors.ServiceDescriptor serviceDescriptor =
                protoManager.getServiceDescriptor(serviceName);
        if (serviceDescriptor == null) {
            throw new IllegalArgumentException("Service not found: " + serviceName);
        }
        System.out.println("Debug: Found service descriptor: " + serviceDescriptor.getFullName());

        // 获取方法描述符
        Descriptors.MethodDescriptor methodDescriptor =
                serviceDescriptor.findMethodByName(methodName);
        if (methodDescriptor == null) {
            throw new IllegalArgumentException(
                    "Method not found: " + methodName + " in service: " + serviceName);
        }
        System.out.println("Debug: Found method descriptor: " + methodDescriptor.getFullName());

        // 创建gRPC方法描述符
        MethodDescriptor<DynamicMessage, DynamicMessage> grpcMethodDescriptor =
                MethodDescriptorHelper.createMethodDescriptor(serviceDescriptor, methodDescriptor);
        if (grpcMethodDescriptor == null) {
            throw new IllegalStateException("Failed to create gRPC method descriptor");
        }
        System.out.println("Debug: Created gRPC method descriptor: " + grpcMethodDescriptor.getFullMethodName());

        // 构建请求消息
        try {
            DynamicMessage.Builder requestBuilder =
                    DynamicMessage.newBuilder(methodDescriptor.getInputType());
            if (requestBuilder == null) {
                throw new IllegalStateException("Failed to create request builder");
            }

            System.out.println("Debug: Parsing JSON: " + requestJson);
            jsonParser.merge(requestJson, requestBuilder);
            DynamicMessage request = requestBuilder.build();
            if (request == null) {
                throw new IllegalStateException("Failed to build request message");
            }
            System.out.println("Debug: Built request message: " + request);

            // 检查channel状态
            if (channel == null) {
                throw new IllegalStateException("Channel is null");
            }
            System.out.println("Debug: Channel state: " + channel.getState(false));

            // 发起调用 - 使用CallOptions.DEFAULT而不是null
            System.out.println("Debug: Making gRPC call...");
            DynamicMessage response = ClientCalls.blockingUnaryCall(
                    channel, grpcMethodDescriptor, CallOptions.DEFAULT, request);

            if (response == null) {
                throw new IllegalStateException("Received null response");
            }
            System.out.println("Debug: Received response: " + response);

            // 转换响应为JSON
            return jsonPrinter.print(response);

        } catch (Exception e) {
            System.err.println("Debug: Exception during request processing: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 调用gRPC方法（使用DynamicMessage）
     */
    public DynamicMessage callMethod(String serviceName, String methodName, DynamicMessage request)
            throws Exception {

        Descriptors.ServiceDescriptor serviceDescriptor =
                protoManager.getServiceDescriptor(serviceName);
        if (serviceDescriptor == null) {
            throw new IllegalArgumentException("Service not found: " + serviceName);
        }

        Descriptors.MethodDescriptor methodDescriptor =
                serviceDescriptor.findMethodByName(methodName);
        if (methodDescriptor == null) {
            throw new IllegalArgumentException(
                    "Method not found: " + methodName + " in service: " + serviceName);
        }

        MethodDescriptor<DynamicMessage, DynamicMessage> grpcMethodDescriptor =
                MethodDescriptorHelper.createMethodDescriptor(serviceDescriptor, methodDescriptor);

        // 使用CallOptions.DEFAULT而不是null
        return ClientCalls.blockingUnaryCall(channel, grpcMethodDescriptor, CallOptions.DEFAULT, request);
    }

    /**
     * 获取服务描述符（添加此方法用于调试）
     */
    public Descriptors.ServiceDescriptor getServiceDescriptor(String serviceName) {
        return protoManager.getServiceDescriptor(serviceName);
    }

    /**
     * 获取可用的服务列表
     */
    public void listServices() {
        System.out.println("Available services:");
        for (String serviceName : protoManager.getAllServiceNames()) {
            System.out.println("  - " + serviceName);

            Descriptors.ServiceDescriptor sd = protoManager.getServiceDescriptor(serviceName);
            if (sd != null) {
                for (Descriptors.MethodDescriptor md : sd.getMethods()) {
                    System.out.println("    * " + md.getName() +
                            "(" + md.getInputType().getName() + ") -> " +
                            md.getOutputType().getName());
                }
            }
        }
    }

    /**
     * 关闭客户端
     */
    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}