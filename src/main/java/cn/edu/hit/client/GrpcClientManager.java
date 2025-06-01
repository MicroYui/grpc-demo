package cn.edu.hit.client;

import java.util.Scanner;

/**
 * gRPC客户端管理器，提供交互式命令行界面
 */
public class GrpcClientManager {
    private DynamicGrpcClient client;
    private Scanner scanner;
    
    public static void main(String[] args) {
        GrpcClientManager manager = new GrpcClientManager();
        manager.start();
    }
    
    public void start() {
        scanner = new Scanner(System.in);
        
        System.out.println("=== gRPC Dynamic Client ===");
        System.out.print("Enter server host (default: localhost): ");
        String host = scanner.nextLine().trim();
        if (host.isEmpty()) {
            host = "localhost";
        }
        
        System.out.print("Enter server port (default: 50051): ");
        String portStr = scanner.nextLine().trim();
        int port = portStr.isEmpty() ? 50051 : Integer.parseInt(portStr);
        
        client = new DynamicGrpcClient(host, port);
        
        // 加载proto文件
        loadProtoFiles();
        
        // 开始交互循环
        commandLoop();
    }
    
    private void loadProtoFiles() {
        try {
            String protoDir = "src/main/resources/proto";
            System.out.println("Loading proto files from: " + protoDir);
            client.loadProtoFiles(protoDir);
            System.out.println("Proto files loaded successfully!");
        } catch (Exception e) {
            System.err.println("Failed to load proto files: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private void commandLoop() {
        printHelp();
        
        while (true) {
            System.out.print("\ngrpc> ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) continue;
            
            String[] parts = input.split("\\s+", 2);
            String command = parts[0].toLowerCase();
            
            try {
                switch (command) {
                    case "help":
                    case "h":
                        printHelp();
                        break;
                        
                    case "list":
                    case "ls":
                        client.listServices();
                        break;
                        
                    case "call":
                        if (parts.length < 2) {
                            System.out.println("Usage: call <service>.<method>");
                            break;
                        }
                        handleCall(parts[1]);
                        break;
                        
                    case "quit":
                    case "exit":
                    case "q":
                        System.out.println("Goodbye!");
                        client.shutdown();
                        System.exit(0);
                        break;
                        
                    default:
                        System.out.println("Unknown command: " + command);
                        System.out.println("Type 'help' for available commands.");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void handleCall(String methodPath) throws Exception {
        // 从最后一个点开始分割，点之前是服务名，点之后是方法名
        int lastDotIndex = methodPath.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == 0 || lastDotIndex == methodPath.length() - 1) {
            System.out.println("Invalid format. Use: <full.service.name>.<method>");
            System.out.println("Example: user.UserService.GetUser");
            return;
        }

        String serviceName = methodPath.substring(0, lastDotIndex);
        String methodName = methodPath.substring(lastDotIndex + 1);

        System.out.println("Calling service: " + serviceName + ", method: " + methodName);

        System.out.println("Enter request JSON (or press Enter for empty request):");
        System.out.print("request> ");
        String requestJson = scanner.nextLine().trim();

        if (requestJson.isEmpty()) {
            requestJson = "{}";
        }

        String response = client.callMethod(serviceName, methodName, requestJson);
        System.out.println("Response:");
        System.out.println(response);
    }

    private void printHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("  help, h          - Show this help message");
        System.out.println("  list, ls         - List all available services and methods");
        System.out.println("  call <full.service.name>.<method> - Call a gRPC method");
        System.out.println("  quit, exit, q    - Exit the client");
        System.out.println("\nExamples:");
        System.out.println("  call user.UserService.GetUser");
        System.out.println("  call user.UserService.CreateUser");
        System.out.println("  call order.OrderService.CreateOrder");
    }
} 