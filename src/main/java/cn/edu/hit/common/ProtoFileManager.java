package cn.edu.hit.common;

import com.github.os72.protocjar.Protoc;
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

        // 构建proto文件名列表
        List<String> protoFileNames = new ArrayList<>();
        for (File protoFile : protoFiles) {
            protoFileNames.add(protoFile.getName());
        }

        // 构建protoc参数
        List<String> args = new ArrayList<>();
        args.add("--descriptor_set_out=" + outputPath);
        args.add("--include_imports");
        args.add("--include_source_info");
        args.add("--proto_path=" + protoDir);
        args.addAll(protoFileNames);

        System.out.println("Compiling proto files: " + protoFileNames);
        System.out.println("Proto path: " + protoDir);
        System.out.println("Output: " + outputPath);

        // 设置工作目录并执行编译
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(new File(protoDir));  // 设置工作目录为proto目录

        int result = Protoc.runProtoc(args.toArray(new String[0]));
        if (result != 0) {
            throw new RuntimeException("Failed to compile proto files");
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