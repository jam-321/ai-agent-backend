package com.jam.agent.monitoring.controller;

import com.jam.agent.agent.service.ImageAttachmentService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理员查看全部会话中的图片；接口路径由 ROLE_ADMIN 统一保护。 */
@RestController
@RequestMapping("/api/admin/attachments")
public class AdminAttachmentController {

    private final ImageAttachmentService images;

    public AdminAttachmentController(ImageAttachmentService images) {
        this.images = images;
    }

    @GetMapping("/{assetId}/content")
    public ResponseEntity<byte[]> content(@PathVariable long assetId) {
        var asset = images.require(assetId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(asset.getContentType()))
                .body(images.read(assetId));
    }
}
