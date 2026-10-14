package com.company.efood.sys.controller;
import com.company.efood.base.BaseResponse;
import com.company.efood.base.BaseUtils;
import com.company.efood.sys.model.FileResponse;
import com.company.efood.sys.services.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

import static com.company.efood.base.BaseConstants.*;

@RestController
@RequestMapping({PRIVET_ENDPOINT + "files", PUBLIC_ENDPOINT + "files"})
public class FileUploadController {

    private final FileService fileService;
    private final BaseUtils baseUtils;

    @Autowired
    public FileUploadController(FileService fileService, BaseUtils baseUtils) {
        this.fileService = fileService;
        this.baseUtils = baseUtils;
    }

    @PostMapping(value = "/upload")
    public ResponseEntity<FileResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            FileResponse response = fileService.storeFile(file);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @PostMapping(value = "/upload/productImages")
    public BaseResponse uploadProductImage(@RequestParam("file") List<MultipartFile> fileList) {
        try {
            List<FileResponse> response = fileService.storeMultipleFile(fileList);
            return baseUtils.generateSuccessResponse(response,UPLOAD_MESSAGE,UPLOAD_MESSAGE_BN);
        } catch (IOException e) {
            return baseUtils.generateErrorResponse(e);
        }
    }

    @GetMapping("/view/{filename:.+}")
    public ResponseEntity<Resource> viewFile(@PathVariable String filename) {
        try {
            Resource resource = fileService.loadFile(filename);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG) // optionally detect type dynamically
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/delete/{filename:.+}")
    public ResponseEntity<Void> deleteFile(@PathVariable String filename) {
        try {
            fileService.deleteFile(filename);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/update/{oldFilename:.+}")
    public ResponseEntity<FileResponse> updateFile(
            @PathVariable String oldFilename,
            @RequestParam("file") MultipartFile newFile) {
        try {
            FileResponse updated = fileService.updateFile(oldFilename, newFile);
            return ResponseEntity.ok(updated);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

