package cn.edu.hit.common;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Proto文件管理器，负责动态编译和加载proto文件
 */
public class ProtoFileManager {
    private final Map<String, Descriptors.FileDescriptor> fileDescriptors = new HashMap<>();
    private final Map<String, Descriptors.ServiceDescriptor> serviceDescriptors = new HashMap<>();

    // 添加 protoc 可执行文件路径配置
    private String protocPath = "protoc"; // 默认从系统路径查找

    public void setProtocPath(String protocPath) {
        this.protocPath = protocPath;
    }

    /**
     * 加载并编译proto文件
     */
    public void loadProtoFiles(String protoDir) throws Exception {
        File dir = new File(protoDir);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalArgumentException("Proto directory not found: " + protoDir);
        }

        // 获取所有proto文件
        File[] protoFiles = dir.listFiles((d, name) -> name.endsWith(".proto"));
        if (protoFiles == null || protoFiles.length == 0) {
            throw new IllegalArgumentException("No proto files found in: " + protoDir);
        }

        // 创建临时目录用于存放编译结果
        Path tempDir = Files.createTempDirectory("grpc-dynamic");

        try {
            // 编译proto文件为descriptor
            compileProtoFiles(protoFiles, tempDir.toString(), dir.getAbsolutePath());

            // 加载descriptor
            loadDescriptors(tempDir.toString());

        } finally {
            // 清理临时文件
            deleteDirectory(tempDir.toFile());
        }
    }

    /**
     * 批量编译proto文件
     */
    private void compileProtoFiles(File[] protoFiles, String outputDir, String protoDir) throws Exception {
        String outputPath = outputDir + File.separator + "all.desc";

        // 构建protoc命令
        List<String> command = new ArrayList<>();
        command.add(protocPath); // protoc可执行文件
        command.add("--descriptor_set_out=" + outputPath); // 输出descriptor文件
        command.add("--include_imports"); // 包含依赖的proto
        command.add("--include_source_info"); // 包含源码信息
        command.add("--proto_path=" + protoDir); // proto文件搜索路径
        
        // 添加所有proto文件
        for (File protoFile : protoFiles) {
            command.add(protoFile.getName());
        }

        System.out.println("Executing protoc command: " + String.join(" ", command));
        System.out.println("Working directory: " + protoDir);

        // 使用 ProcessBuilder 执行命令
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(protoDir));
        processBuilder.redirectErrorStream(true); // 将错误流重定向到标准输出

        Process process = processBuilder.start();

        // 读取输出
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("protoc output: " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Failed to compile proto files, exit code: " + exitCode);
        }
    }

    /**
     * 加载编译后的descriptor文件
     */
    private void loadDescriptors(String descriptorDir) throws Exception {
        File dir = new File(descriptorDir);
        File[] descFiles = dir.listFiles((d, name) -> name.endsWith(".desc"));

        if (descFiles == null) return;

        for (File descFile : descFiles) {
            try (FileInputStream fis = new FileInputStream(descFile)) {
                DescriptorProtos.FileDescriptorSet descriptorSet =
                        DescriptorProtos.FileDescriptorSet.parseFrom(fis);

                for (DescriptorProtos.FileDescriptorProto fdp : descriptorSet.getFileList()) {
                    Descriptors.FileDescriptor fd = buildFileDescriptor(fdp);
                    fileDescriptors.put(fd.getFullName(), fd);

                    // 提取服务描述符
                    for (Descriptors.ServiceDescriptor sd : fd.getServices()) {
                        serviceDescriptors.put(sd.getFullName(), sd);
                    }
                }
            }
        }
    }

    /**
     * 构建FileDescriptor
     */
    private Descriptors.FileDescriptor buildFileDescriptor(DescriptorProtos.FileDescriptorProto fdp)
            throws Descriptors.DescriptorValidationException {

        List<Descriptors.FileDescriptor> dependencies = new ArrayList<>();

        for (String depName : fdp.getDependencyList()) {
            Descriptors.FileDescriptor dep = fileDescriptors.get(depName);
            if (dep != null) {
                dependencies.add(dep);
            }
        }

        return Descriptors.FileDescriptor.buildFrom(
                fdp,
                dependencies.toArray(new Descriptors.FileDescriptor[0])
        );
    }

    /**
     * 获取服务描述符
     */
    public Descriptors.ServiceDescriptor getServiceDescriptor(String serviceName) {
        return serviceDescriptors.get(serviceName);
    }

    /**
     * 获取所有服务名称
     */
    public Set<String> getAllServiceNames() {
        return new HashSet<>(serviceDescriptors.keySet());
    }

    /**
     * 删除目录及其内容
     */
    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        dir.delete();
    }
}