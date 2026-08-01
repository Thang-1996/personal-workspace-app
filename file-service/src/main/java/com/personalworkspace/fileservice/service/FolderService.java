package com.personalworkspace.fileservice.service;
import com.personalworkspace.fileservice.dto.FolderDtos.*;
import com.personalworkspace.fileservice.entity.Folder;
import com.personalworkspace.fileservice.exception.FileApiException;
import com.personalworkspace.fileservice.repository.FolderRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class FolderService {
 private final FolderRepository repository;
 @Transactional public Response create(UUID owner,Request request){Folder parent=request.parentId()==null?null:required(owner,request.parentId());ensureUnique(owner,request.parentId(),request.name());return Response.from(repository.save(Folder.create(owner,request.name(),parent)));}
 public List<Response> list(UUID owner,UUID parentId){List<Folder> values=parentId==null?repository.findAllByOwnerIdAndParentIsNullOrderByName(owner):repository.findAllByOwnerIdAndParentIdOrderByName(owner,parentId);return values.stream().map(Response::from).toList();}
 @Transactional public Response patch(UUID owner,UUID id,Patch request){Folder f=required(owner,id);ensureUnique(owner,f.getParent()==null?null:f.getParent().getId(),request.name());f.rename(request.name());return Response.from(f);}
 private Folder required(UUID o,UUID id){return repository.findByIdAndOwnerId(id,o).orElseThrow(()->new FileApiException(HttpStatus.NOT_FOUND,"Folder not found"));}
 private void ensureUnique(UUID o,UUID p,String n){boolean exists=p==null?repository.existsByOwnerIdAndParentIsNullAndNameIgnoreCase(o,n.trim()):repository.existsByOwnerIdAndParentIdAndNameIgnoreCase(o,p,n.trim());if(exists)throw new FileApiException(HttpStatus.CONFLICT,"Folder name already exists");}
}
