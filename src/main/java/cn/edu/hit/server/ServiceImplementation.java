package cn.edu.hit.server;

import com.google.protobuf.DynamicMessage;
import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 模拟服务实现，用于演示动态gRPC调用
 */
public class ServiceImplementation {
    private final Map<Integer, Map<String, Object>> users = new HashMap<>();
    private final AtomicInteger userIdCounter = new AtomicInteger(1);
    
    public ServiceImplementation() {
        // 初始化一些测试数据
        initTestData();
    }
    
    private void initTestData() {
        addUser("Alice", "alice@example.com", 25);
        addUser("Bob", "bob@example.com", 30);
        addUser("Charlie", "charlie@example.com", 35);
    }
    
    private void addUser(String name, String email, int age) {
        int id = userIdCounter.getAndIncrement();
        Map<String, Object> user = new HashMap<>();
        user.put("id", id);
        user.put("name", name);
        user.put("email", email);
        user.put("age", age);
        user.put("created_time", System.currentTimeMillis());
        users.put(id, user);
    }
    
    /**
     * 处理GetUser请求
     */
    public void getUser(DynamicMessage request, StreamObserver<DynamicMessage> responseObserver) {
        try {
            int userId = (Integer) request.getField(
                request.getDescriptorForType().findFieldByName("user_id"));
            
            Map<String, Object> userData = users.get(userId);
            
            DynamicMessage.Builder responseBuilder = DynamicMessage.newBuilder(
                request.getDescriptorForType()
                    .getFile()
                    .findMessageTypeByName("GetUserResponse"));
            
            if (userData != null) {
                // 构建User消息
                DynamicMessage.Builder userBuilder = DynamicMessage.newBuilder(
                    request.getDescriptorForType()
                        .getFile()
                        .findMessageTypeByName("User"));
                
                userBuilder.setField(
                    userBuilder.getDescriptorForType().findFieldByName("id"), 
                    userData.get("id"));
                userBuilder.setField(
                    userBuilder.getDescriptorForType().findFieldByName("name"), 
                    userData.get("name"));
                userBuilder.setField(
                    userBuilder.getDescriptorForType().findFieldByName("email"), 
                    userData.get("email"));
                userBuilder.setField(
                    userBuilder.getDescriptorForType().findFieldByName("age"), 
                    userData.get("age"));
                userBuilder.setField(
                    userBuilder.getDescriptorForType().findFieldByName("created_time"), 
                    userData.get("created_time"));
                
                responseBuilder.setField(
                    responseBuilder.getDescriptorForType().findFieldByName("user"), 
                    userBuilder.build());
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
    
    /**
     * 处理CreateUser请求
     */
    public void createUser(DynamicMessage request, StreamObserver<DynamicMessage> responseObserver) {
        try {
            String name = (String) request.getField(
                request.getDescriptorForType().findFieldByName("name"));
            String email = (String) request.getField(
                request.getDescriptorForType().findFieldByName("email"));
            int age = (Integer) request.getField(
                request.getDescriptorForType().findFieldByName("age"));
            
            // 创建新用户
            int id = userIdCounter.getAndIncrement();
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", id);
            userData.put("name", name);
            userData.put("email", email);
            userData.put("age", age);
            userData.put("created_time", System.currentTimeMillis());
            users.put(id, userData);
            
            // 构建响应
            DynamicMessage.Builder responseBuilder = DynamicMessage.newBuilder(
                request.getDescriptorForType()
                    .getFile()
                    .findMessageTypeByName("CreateUserResponse"));
            
            // 构建User消息
            DynamicMessage.Builder userBuilder = DynamicMessage.newBuilder(
                request.getDescriptorForType()
                    .getFile()
                    .findMessageTypeByName("User"));
            
            userBuilder.setField(
                userBuilder.getDescriptorForType().findFieldByName("id"), id);
            userBuilder.setField(
                userBuilder.getDescriptorForType().findFieldByName("name"), name);
            userBuilder.setField(
                userBuilder.getDescriptorForType().findFieldByName("email"), email);
            userBuilder.setField(
                userBuilder.getDescriptorForType().findFieldByName("age"), age);
            userBuilder.setField(
                userBuilder.getDescriptorForType().findFieldByName("created_time"), 
                userData.get("created_time"));
            
            responseBuilder.setField(
                responseBuilder.getDescriptorForType().findFieldByName("user"), 
                userBuilder.build());
            responseBuilder.setField(
                responseBuilder.getDescriptorForType().findFieldByName("success"), true);
            responseBuilder.setField(
                responseBuilder.getDescriptorForType().findFieldByName("message"), 
                "User created successfully");
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            responseObserver.onError(e);
        }
    }
} 