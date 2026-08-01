package com.personalworkspace.fileservice.configuration;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
@Configuration(proxyBeanMethods=false)
public class StorageConfiguration {
 @Bean MinioClient minioClient(@Value("${storage.endpoint}")String endpoint,@Value("${storage.access-key}")String access,@Value("${storage.secret-key}")String secret){return MinioClient.builder().endpoint(endpoint).credentials(access,secret).build();}
}
