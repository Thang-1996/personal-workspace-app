package com.personalworkspace.fileservice.service;
import com.personalworkspace.fileservice.dto.FileResponse;
import com.personalworkspace.fileservice.entity.*;
import com.personalworkspace.fileservice.exception.FileApiException;
import com.personalworkspace.fileservice.event.FileEvents;
import com.personalworkspace.fileservice.repository.*;
import com.personalworkspace.fileservice.storage.ObjectStorage;
import java.io.*;
import java.security.*;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
@Service @RequiredArgsConstructor
public class FileApplicationService {
 private final StoredFileRepository files; private final FolderRepository folders; private final FileLinkRepository links; private final ObjectStorage storage; private final ApplicationEventPublisher events;
 private final Tika tika=new Tika();
 @Value("${storage.max-size-bytes}") long maxSize;
 @Value("${storage.allowed-types}") Set<String> allowedTypes;
 @Transactional
 public FileResponse upload(UUID owner,MultipartFile part,UUID folderId){
  if(part.isEmpty()||part.getSize()>maxSize)throw new FileApiException(HttpStatus.PAYLOAD_TOO_LARGE,"File is empty or exceeds configured limit");
  try{
   byte[] bytes=part.getBytes();String detected=tika.detect(bytes,part.getOriginalFilename());
   if(!allowedTypes.contains(detected))throw new FileApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"Detected MIME type is not allowed: "+detected);
   Folder folder=folderId==null?null:folder(owner,folderId);
   String key=owner+"/"+UUID.randomUUID();StoredFile entity=StoredFile.pending(owner,safeName(part.getOriginalFilename()),key,detected,bytes.length,sha256(bytes),folder);
   files.saveAndFlush(entity);
   try{storage.put(key,new ByteArrayInputStream(bytes),bytes.length,detected);entity.ready();files.saveAndFlush(entity);events.publishEvent(new FileEvents.FileUploaded(entity.getId(),owner,detected,bytes.length,Instant.now()));}
   catch(Exception storageFailure){try{storage.delete(key);}catch(Exception ignored){}files.delete(entity);throw storageFailure;}
   return FileResponse.from(entity);
  }catch(FileApiException e){throw e;}catch(Exception e){throw new FileApiException(HttpStatus.BAD_GATEWAY,"Object storage upload failed");}
 }
 @Transactional(readOnly=true)
 public List<FileResponse> list(UUID owner,UUID folderId,String name,String type){
  List<StoredFile> result=folderId==null?files.findAllByOwnerIdAndStatusOrderByCreatedAtDesc(owner,FileStatus.READY):files.findAllByOwnerIdAndFolderIdAndStatusOrderByCreatedAtDesc(owner,folderId,FileStatus.READY);
  return result.stream().filter(f->name==null||f.getOriginalName().toLowerCase().contains(name.toLowerCase())).filter(f->type==null||f.getContentType().startsWith(type)).map(FileResponse::from).toList();
 }
 @Transactional(readOnly=true)
 public ResponseEntity<InputStreamResource> download(UUID owner,UUID id){
  StoredFile f=required(owner,id);try{return ResponseEntity.ok().contentType(MediaType.parseMediaType(f.getContentType())).contentLength(f.getSizeBytes())
    .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(f.getOriginalName()).build().toString()).body(new InputStreamResource(storage.get(f.getStorageKey())));
  }catch(Exception e){throw new FileApiException(HttpStatus.BAD_GATEWAY,"Object storage download failed");}}
 @Transactional public void delete(UUID owner,UUID id){StoredFile f=required(owner,id);try{storage.delete(f.getStorageKey());f.deleted();events.publishEvent(new FileEvents.FileDeleted(f.getId(),owner,Instant.now()));}catch(Exception e){throw new FileApiException(HttpStatus.BAD_GATEWAY,"Object delete failed; metadata was preserved for retry");}}
 @Transactional public void linkTask(UUID owner,UUID id,UUID taskId){StoredFile f=required(owner,id);if(!links.existsByFileIdAndEntityTypeAndEntityId(id,"TASK",taskId))links.save(FileLink.task(f,taskId));}
 private StoredFile required(UUID owner,UUID id){return files.findByIdAndOwnerIdAndStatus(id,owner,FileStatus.READY).orElseThrow(()->new FileApiException(HttpStatus.NOT_FOUND,"File not found"));}
 private Folder folder(UUID owner,UUID id){return folders.findByIdAndOwnerId(id,owner).orElseThrow(()->new FileApiException(HttpStatus.NOT_FOUND,"Folder not found"));}
 private String safeName(String name){String value=name==null?"file":name.replace('\\','/');return value.substring(value.lastIndexOf('/')+1);}
 private String sha256(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
}
