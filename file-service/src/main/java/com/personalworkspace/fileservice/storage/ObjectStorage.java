package com.personalworkspace.fileservice.storage;
import io.minio.*;
import java.io.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class ObjectStorage {
 private final MinioClient client; private final String bucket;
 public ObjectStorage(MinioClient client,@Value("${storage.bucket}")String bucket){this.client=client;this.bucket=bucket;}
 public void ensureBucket()throws Exception{if(!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()))client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());}
 public void put(String key,InputStream in,long size,String type)throws Exception{ensureBucket();client.putObject(PutObjectArgs.builder().bucket(bucket).object(key).stream(in,size,-1).contentType(type).build());}
 public InputStream get(String key)throws Exception{return client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build());}
 public void delete(String key)throws Exception{client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());}
}
