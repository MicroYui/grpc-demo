package cn.edu.hit.common;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import io.grpc.MethodDescriptor;
import io.grpc.protobuf.ProtoUtils;

/**
 * 方法描述符辅助类，用于创建gRPC方法描述符
 */
public class MethodDescriptorHelper {
    
    /**
     * 创建gRPC方法描述符
     */
    public static MethodDescriptor<DynamicMessage, DynamicMessage> createMethodDescriptor(
            Descriptors.ServiceDescriptor serviceDescriptor,
            Descriptors.MethodDescriptor methodDescriptor) {
        
        String fullMethodName = generateFullMethodName(
            serviceDescriptor.getFullName(), 
            methodDescriptor.getName()
        );
        
        return MethodDescriptor.<DynamicMessage, DynamicMessage>newBuilder()
            .setType(getMethodType(methodDescriptor))
            .setFullMethodName(fullMethodName)
            .setRequestMarshaller(ProtoUtils.marshaller(
                DynamicMessage.getDefaultInstance(methodDescriptor.getInputType())
            ))
            .setResponseMarshaller(ProtoUtils.marshaller(
                DynamicMessage.getDefaultInstance(methodDescriptor.getOutputType())
            ))
            .build();
    }
    
    /**
     * 生成完整方法名
     */
    private static String generateFullMethodName(String serviceName, String methodName) {
        return serviceName + "/" + methodName;
    }
    
    /**
     * 获取方法类型
     */
    private static MethodDescriptor.MethodType getMethodType(Descriptors.MethodDescriptor method) {
        if (method.isClientStreaming() && method.isServerStreaming()) {
            return MethodDescriptor.MethodType.BIDI_STREAMING;
        } else if (method.isClientStreaming()) {
            return MethodDescriptor.MethodType.CLIENT_STREAMING;
        } else if (method.isServerStreaming()) {
            return MethodDescriptor.MethodType.SERVER_STREAMING;
        } else {
            return MethodDescriptor.MethodType.UNARY;
        }
    }
} 