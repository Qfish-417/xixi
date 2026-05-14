package com.xixi.manjuagent.service;

import com.xixi.manjuagent.config.MinioConfig;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    public void initBucket(String bucketName) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("创建Bucket: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("初始化Bucket失败: {}", e.getMessage());
        }
    }

    public String uploadImage(MultipartFile file) {
        return uploadFile(file, minioConfig.getImageBucket());
    }

    public String uploadVideo(MultipartFile file) {
        return uploadFile(file, minioConfig.getVideoBucket());
    }

    public String uploadFile(MultipartFile file, String bucketName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        try {
            initBucket(bucketName);
            
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            String objectName = UUID.randomUUID().toString() + extension;

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            log.info("文件上传成功: bucket={}, object={}", bucketName, objectName);
            return objectName;
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage());
            throw new RuntimeException("文件上传失败", e);
        }
    }

    public String uploadFile(InputStream inputStream, String filename, String contentType, long size) {
        return uploadFile(inputStream, filename, contentType, size, minioConfig.getImageBucket());
    }

    public String uploadVideo(InputStream inputStream, String filename, String contentType, long size) {
        return uploadFile(inputStream, filename, contentType, size, minioConfig.getVideoBucket());
    }

    private String uploadFile(InputStream inputStream, String filename, String contentType, long size, String bucketName) {
        try {
            initBucket(bucketName);

            String extension = "";
            if (filename != null && filename.contains(".")) {
                extension = filename.substring(filename.lastIndexOf("."));
            }
            
            String objectName = UUID.randomUUID().toString() + extension;

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());

            log.info("文件上传成功: bucket={}, object={}", bucketName, objectName);
            return objectName;
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage());
            throw new RuntimeException("文件上传失败", e);
        }
    }

    public InputStream downloadFile(String bucketName, String objectName) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            log.error("文件下载失败: {}", e.getMessage());
            throw new RuntimeException("文件下载失败", e);
        }
    }

    public InputStream downloadImage(String objectName) {
        return downloadFile(minioConfig.getImageBucket(), objectName);
    }

    public void deleteFile(String bucketName, String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            log.info("文件删除成功: bucket={}, object={}", bucketName, objectName);
        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage());
            throw new RuntimeException("文件删除失败", e);
        }
    }

    public void deleteImage(String objectName) {
        deleteFile(minioConfig.getImageBucket(), objectName);
    }

    public void deleteVideo(String objectName) {
        deleteFile(minioConfig.getVideoBucket(), objectName);
    }

    public String getPresignedUrl(String bucketName, String objectName, int expiresMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(expiresMinutes, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            log.error("获取预签名URL失败: {}", e.getMessage());
            throw new RuntimeException("获取预签名URL失败", e);
        }
    }

    public String getImageUrl(String objectName) {
        return getPresignedUrl(minioConfig.getImageBucket(), objectName, 1440);
    }

    public String getVideoUrl(String objectName) {
        return getPresignedUrl(minioConfig.getVideoBucket(), objectName, 1440);
    }

    public String getPublicUrl(String objectName) {
        return String.format("%s/%s/%s", minioConfig.getEndpoint(), minioConfig.getImageBucket(), objectName);
    }

    /**
     * 直接上传字节数组到MinIO，返回预签名URL
     */
    public String uploadBytes(byte[] data, String fileExtension) {
        try {
            String bucketName = minioConfig.getImageBucket();
            initBucket(bucketName);

            String ext = (fileExtension != null && !fileExtension.isEmpty()) ? fileExtension : "png";
            String objectName = UUID.randomUUID().toString() + "." + ext;

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(new ByteArrayInputStream(data), data.length, -1)
                    .contentType("image/" + ext)
                    .build());

            log.info("字节上传成功: object={}, size={}bytes", objectName, data.length);
            return getImageUrl(objectName);
        } catch (Exception e) {
            log.error("字节上传失败: {}", e.getMessage());
            throw new RuntimeException("图片存储失败", e);
        }
    }

    /**
     * 从URL下载文件并上传到MinIO，返回预签名URL
     */
    public String uploadFromUrl(String fileUrl, String fileExtension) {
        try {
            URL url = new URL(fileUrl);
            byte[] data;
            try (InputStream in = url.openStream()) {
                data = in.readAllBytes();
            }

            // 根据扩展名判断是图片还是视频
            String bucketName;
            String contentType;
            String objectName;
            
            if ("mp4".equalsIgnoreCase(fileExtension)) {
                bucketName = minioConfig.getVideoBucket();
                contentType = "video/mp4";
                objectName = UUID.randomUUID().toString() + ".mp4";
            } else {
                bucketName = minioConfig.getImageBucket();
                contentType = "image/" + (fileExtension != null ? fileExtension : "png");
                objectName = UUID.randomUUID().toString()
                        + (fileExtension != null && !fileExtension.isEmpty() ? "." + fileExtension : ".png");
            }
            
            initBucket(bucketName);

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(new ByteArrayInputStream(data), data.length, -1)
                    .contentType(contentType)
                    .build());

            log.info("从URL下载并上传成功: url={}, object={}", fileUrl, objectName);

            // 返回对应的URL
            if ("mp4".equalsIgnoreCase(fileExtension)) {
                return getVideoUrl(objectName);
            }
            return getImageUrl(objectName);
        } catch (Exception e) {
            log.error("从URL下载并上传失败: url={}, error={}", fileUrl, e.getMessage());
            throw new RuntimeException("文件存储失败", e);
        }
    }
}
