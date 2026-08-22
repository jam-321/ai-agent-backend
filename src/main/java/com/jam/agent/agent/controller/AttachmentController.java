package com.jam.agent.agent.controller;

import com.jam.agent.agent.service.ImageAttachmentService;
import com.jam.agent.auth.security.AuthenticatedUser;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 通过登录态读取图片，避免向模型和浏览器暴露永久公开对象地址。 */
@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    private final ImageAttachmentService images;

    public AttachmentController(ImageAttachmentService images) { this.images = images; }

    @GetMapping("/{assetId}/content")
    public ResponseEntity<byte[]> content(
            @PathVariable long assetId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        var asset = images.requireOwned(user.id(), assetId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(asset.getContentType()))
                .body(images.readOwned(user.id(), assetId));
    }
}
