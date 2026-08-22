package com.jam.agent.agent.service;

import com.jam.agent.agent.persistence.entity.MediaAssetEntity;
import com.jam.agent.agent.persistence.repository.ConversationTurnAttachmentRepository;
import com.jam.agent.agent.persistence.repository.MediaAssetRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** 负责图片校验、MinIO 持久化和会话轮次关联。Base64 只在调用模型时短暂生成。 */
@Service
public class ImageAttachmentService {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final List<String> IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/gif", "image/webp");

    private final MinioClient minio;
    private final MediaAssetRepository assets;
    private final ConversationTurnAttachmentRepository turnAttachments;
    private final String bucket;

    public ImageAttachmentService(
            MinioClient minio,
            MediaAssetRepository assets,
            ConversationTurnAttachmentRepository turnAttachments,
            @Value("${app.storage.minio.bucket}") String bucket) {
        this.minio = minio;
        this.assets = assets;
        this.turnAttachments = turnAttachments;
        this.bucket = bucket;
        try {
            if (!minio.bucketExists(io.minio.BucketExistsArgs.builder().bucket(bucket).build())) {
                minio.makeBucket(io.minio.MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception exception) {
            throw new IllegalStateException("MinIO 存储不可用，请确认 MinIO 已启动。", exception);
        }
    }

    public List<Long> store(long userId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return List.of();
        return files.stream().map(file -> storeOne(userId, file)).toList();
    }

    private long storeOne(long userId, MultipartFile file) {
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("仅支持 JPEG、PNG、GIF 和 WebP 图片。");
        }
        if (file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("图片不能为空且不能超过 20MB。");
        }
        try {
            byte[] bytes = file.getBytes();
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) throw new IllegalArgumentException("图片内容无效。");
            String key = userId + "/" + LocalDateTime.now().toLocalDate() + "/" + UUID.randomUUID();
            minio.putObject(PutObjectArgs.builder()
                    .bucket(bucket).object(key).contentType(contentType)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1).build());

            MediaAssetEntity entity = new MediaAssetEntity();
            entity.setOwnerId(userId);
            entity.setAssetType("IMAGE");
            entity.setStorageKey(key);
            entity.setOriginalFilename(file.getOriginalFilename());
            entity.setContentType(contentType);
            entity.setFileSize((long) bytes.length);
            entity.setSha256(sha256(bytes));
            entity.setWidth(image.getWidth());
            entity.setHeight(image.getHeight());
            entity.setStatus("PENDING");
            return assets.save(entity).getId();
        } catch (IOException exception) {
            throw new IllegalArgumentException("读取图片失败。", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("保存图片失败。", exception);
        }
    }

    public void bind(long conversationId, int turnId, List<Long> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) return;
        turnAttachments.bind(conversationId, turnId, assetIds);
        assetIds.forEach(assets::markBound);
    }

    public List<Long> findCurrentAssetIds(long userId, long conversationId, int turnId) {
        return turnAttachments.findAssetIds(userId, conversationId, turnId);
    }

    public List<ConversationTurnAttachmentRepository.AttachmentRecord> findHistory(
            long userId, long conversationId, int currentTurnId) {
        return turnAttachments.findHistory(userId, conversationId, currentTurnId);
    }

    public Media toMedia(long userId, long assetId) {
        MediaAssetEntity asset = assets.findOwned(userId, assetId)
                .orElseThrow(() -> new IllegalArgumentException("图片不存在或无权访问。"));
        try (InputStream stream = minio.getObject(GetObjectArgs.builder().bucket(bucket).object(asset.getStorageKey()).build())) {
            return Media.builder()
                    .mimeType(MediaType.parseMediaType(asset.getContentType()))
                    .data(stream.readAllBytes())
                    .name("image-" + assetId)
                    .build();
        } catch (Exception exception) {
            throw new IllegalStateException("读取图片失败。", exception);
        }
    }

    public byte[] readOwned(long userId, long assetId) {
        MediaAssetEntity asset = assets.findOwned(userId, assetId)
                .orElseThrow(() -> new IllegalArgumentException("图片不存在或无权访问。"));
        try (InputStream stream = minio.getObject(GetObjectArgs.builder().bucket(bucket).object(asset.getStorageKey()).build())) {
            return stream.readAllBytes();
        } catch (Exception exception) {
            throw new IllegalStateException("读取图片失败。", exception);
        }
    }

    public MediaAssetEntity require(long assetId) {
        return assets.find(assetId)
                .orElseThrow(() -> new IllegalArgumentException("图片不存在。"));
    }

    public byte[] read(long assetId) {
        MediaAssetEntity asset = require(assetId);
        try (InputStream stream = minio.getObject(GetObjectArgs.builder().bucket(bucket).object(asset.getStorageKey()).build())) {
            return stream.readAllBytes();
        } catch (Exception exception) {
            throw new IllegalStateException("读取图片失败。", exception);
        }
    }

    public MediaAssetEntity requireOwned(long userId, long assetId) {
        return assets.findOwned(userId, assetId)
                .orElseThrow(() -> new IllegalArgumentException("图片不存在或无权访问。"));
    }

    public void saveSummary(long assetId, String summary, String modelName) {
        assets.saveSummary(assetId, summary, modelName);
    }

    private String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
