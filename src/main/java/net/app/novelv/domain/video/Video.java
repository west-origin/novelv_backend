package net.app.novelv.domain.video;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "videos")
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "video_id")
    private Long id;

    @Column(name = "r2_object_key", nullable = false, unique = true, length = 1024)
    private String r2ObjectKey;

    @Column(name = "thumbnail_object_key", length = 1024)
    private String thumbnailObjectKey;

    @Column(name = "thumbnail_content_type", length = 100)
    private String thumbnailContentType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "uploader_id", nullable = false)
    private Long uploaderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VideoStatus status = VideoStatus.UPLOAD_URL_CREATED;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Video(
            String r2ObjectKey,
            String title,
            String originalFileName,
            String contentType,
            Long fileSizeBytes,
            Long durationSeconds,
            Long uploaderId
    ) {
        this.r2ObjectKey = r2ObjectKey;
        this.title = title;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.durationSeconds = durationSeconds;
        this.uploaderId = uploaderId;
    }

    public static Video createPendingUpload(
            String r2ObjectKey,
            String title,
            String originalFileName,
            String contentType,
            Long fileSizeBytes,
            Long durationSeconds,
            Long uploaderId
    ) {
        return new Video(
                r2ObjectKey,
                title,
                originalFileName,
                contentType,
                fileSizeBytes,
                durationSeconds,
                uploaderId
        );
    }

    public void markUploadedAsDraft(Long durationSeconds) {
        this.status = VideoStatus.DRAFT;
        if (durationSeconds != null) {
            this.durationSeconds = durationSeconds;
        }
    }

    public void updateTitle(String title) {
        if (StringUtils.hasText(title)) {
            this.title = title.trim();
        }
    }

    public void markReady(Long durationSeconds) {
        this.status = VideoStatus.READY;
        if (durationSeconds != null) {
            this.durationSeconds = durationSeconds;
        }
    }

    public void updateThumbnail(String thumbnailObjectKey, String thumbnailContentType) {
        if (StringUtils.hasText(thumbnailObjectKey)) {
            this.thumbnailObjectKey = thumbnailObjectKey;
            this.thumbnailContentType = StringUtils.hasText(thumbnailContentType)
                    ? thumbnailContentType
                    : "image/webp";
        }
    }

    public void markCancelled() {
        this.status = VideoStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    public boolean isCancelled() {
        return this.status == VideoStatus.CANCELLED;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = VideoStatus.UPLOAD_URL_CREATED;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
