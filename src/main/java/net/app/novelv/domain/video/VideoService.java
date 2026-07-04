package net.app.novelv.domain.video;

import lombok.RequiredArgsConstructor;
import net.app.novelv.domain.user.User;
import net.app.novelv.domain.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VideoService {

    private static final Logger log = LoggerFactory.getLogger(VideoService.class);
    private static final List<VideoStatus> TEMPORARY_UPLOAD_STATUSES = List.of(
            VideoStatus.UPLOAD_URL_CREATED,
            VideoStatus.CANCELLED
    );

    private final CloudflareR2Client cloudflareR2Client;
    private final CloudflareR2Properties properties;
    private final FfmpegThumbnailExtractor thumbnailExtractor;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    @Transactional
    public DirectUploadResponse createDirectUpload(DirectUploadRequest request, Long uploaderId) {
        R2DirectUpload upload = cloudflareR2Client.createDirectUpload(request, uploaderId);

        Video video = Video.createPendingUpload(
                upload.objectKey(),
                request.title(),
                request.fileName(),
                request.contentType(),
                request.fileSizeBytes(),
                request.durationSeconds(),
                uploaderId
        );
        Video savedVideo = videoRepository.save(video);

        return new DirectUploadResponse(
                savedVideo.getId(),
                savedVideo.getR2ObjectKey(),
                upload.uploadUrl(),
                upload.expiresAt()
        );
    }

    @Transactional
    public R2DirectUpload createThumbnailUpload(Long videoId, Long uploaderId, ThumbnailUploadRequest request) {
        Video video = videoRepository.findByIdAndUploaderId(videoId, uploaderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found."));
        if (video.isCancelled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cancelled upload cannot receive a thumbnail.");
        }

        R2DirectUpload upload = cloudflareR2Client.createThumbnailUpload(request, uploaderId);
        video.updateThumbnail(upload.objectKey(), request.contentType());
        return upload;
    }

    @Transactional
    public void completeUpload(Long videoId, Long uploaderId, CompleteUploadRequest request) {
        Video video = videoRepository.findByIdAndUploaderId(videoId, uploaderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found."));
        if (video.isCancelled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cancelled upload cannot be completed.");
        }
        if (request != null) {
            video.updateTitle(request.title());
            validateRequestedThumbnail(video, request);
        }
        ensureThumbnail(video);
        video.markReady(request == null ? null : request.durationSeconds());
    }

    @Transactional
    public void cancelUpload(Long videoId, Long uploaderId) {
        videoRepository.findByIdAndUploaderId(videoId, uploaderId)
                .ifPresent(this::deleteTemporaryUpload);
    }

    private void deleteTemporaryUpload(Video video) {
        if (!TEMPORARY_UPLOAD_STATUSES.contains(video.getStatus())) {
            return;
        }

        cloudflareR2Client.deleteObject(video.getR2ObjectKey());
        cloudflareR2Client.deleteObject(video.getThumbnailObjectKey());
        videoRepository.deleteByIdAndUploaderIdAndStatusIn(
                video.getId(),
                video.getUploaderId(),
                TEMPORARY_UPLOAD_STATUSES
        );
    }

    @Transactional
    public int cleanupAbandonedUploads() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(properties.resolvedCleanupGraceMinutes());
        List<Video> candidates = videoRepository.findTop50ByStatusInAndUpdatedAtBeforeOrderByUpdatedAtAsc(
                TEMPORARY_UPLOAD_STATUSES,
                cutoff
        );

        int cleaned = 0;
        for (Video video : candidates.stream().limit(properties.resolvedCleanupBatchSize()).toList()) {
            try {
                deleteTemporaryUpload(video);
                cleaned++;
            } catch (RuntimeException exception) {
                log.warn("Failed to cleanup abandoned video upload. videoId={}, objectKey={}",
                        video.getId(), video.getR2ObjectKey(), exception);
            }
        }
        return cleaned;
    }

    @Transactional(readOnly = true)
    public List<VideoListResponse> findReadyVideos(Long viewerId) {
        List<Video> videos = videoRepository.findTop20ByStatusOrderByCreatedAtDesc(VideoStatus.READY);
        Map<Long, User> uploadersById = userRepository.findAllById(
                        videos.stream()
                                .map(Video::getUploaderId)
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return videos.stream()
                .map(video -> new VideoListResponse(
                        video.getId(),
                        video.getTitle(),
                        resolveUploaderNickname(video, uploadersById),
                        formatDuration(video.getDurationSeconds()),
                        "NEW",
                        video.getR2ObjectKey(),
                        cloudflareR2Client.createPlaybackUrl(video.getR2ObjectKey(), video.getContentType()),
                        createThumbnailUrl(video)
                ))
                .toList();
    }

    private void ensureThumbnail(Video video) {
        if (video.getThumbnailObjectKey() != null && !video.getThumbnailObjectKey().isBlank()) {
            return;
        }

        String inputUrl = cloudflareR2Client.createPlaybackUrl(video.getR2ObjectKey(), video.getContentType());
        thumbnailExtractor.extractFirstFrame(inputUrl, video.getId())
                .ifPresent(thumbnail -> {
                    String thumbnailObjectKey = cloudflareR2Client.createGeneratedThumbnailObjectKey(
                            video.getUploaderId(),
                            video.getId()
                    );
                    try {
                        cloudflareR2Client.uploadObject(thumbnailObjectKey, thumbnail.file(), thumbnail.contentType());
                        video.updateThumbnail(thumbnailObjectKey, thumbnail.contentType());
                    } catch (RuntimeException exception) {
                        log.warn("Failed to upload generated thumbnail. videoId={}, thumbnailObjectKey={}",
                                video.getId(), thumbnailObjectKey, exception);
                    } finally {
                        deleteTemporaryThumbnail(thumbnail);
                    }
                });
    }

    private void deleteTemporaryThumbnail(FfmpegThumbnailExtractor.ExtractedThumbnail thumbnail) {
        try {
            Files.deleteIfExists(thumbnail.file());
        } catch (IOException exception) {
            log.debug("Failed to delete temporary FFmpeg thumbnail file. path={}", thumbnail.file(), exception);
        }
    }

    private String createThumbnailUrl(Video video) {
        if (video.getThumbnailObjectKey() == null || video.getThumbnailObjectKey().isBlank()) {
            return null;
        }
        return cloudflareR2Client.createPlaybackUrl(video.getThumbnailObjectKey(), video.getThumbnailContentType());
    }

    private void validateRequestedThumbnail(Video video, CompleteUploadRequest request) {
        if (request.thumbnailObjectKey() == null || request.thumbnailObjectKey().isBlank()) {
            return;
        }
        if (!request.thumbnailObjectKey().equals(video.getThumbnailObjectKey())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thumbnail upload was not registered for this video.");
        }
    }

    @Transactional(readOnly = true)
    public PlaybackTokenResponse createPlaybackToken(Long videoId, Long viewerId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found."));

        assertCanWatch(video, viewerId);

        String playbackUrl = cloudflareR2Client.createPlaybackUrl(video.getR2ObjectKey(), video.getContentType());

        return new PlaybackTokenResponse(
                video.getId(),
                video.getR2ObjectKey(),
                playbackUrl,
                properties.resolvedPlaybackUrlTtlSeconds()
        );
    }

    private String resolveUploaderNickname(Video video, Map<Long, User> uploadersById) {
        User uploader = uploadersById.get(video.getUploaderId());
        if (uploader == null || uploader.getNickname() == null || uploader.getNickname().isBlank()) {
            return "Unknown creator";
        }
        return uploader.getNickname();
    }

    private void assertCanWatch(Video video, Long viewerId) {
        // TODO: Replace this with your real purchase/subscription entitlement check.
        // Current rule: any authenticated user passes the placeholder gate.
        if (viewerId == null) {
            throw new AccessDeniedException("Login is required to watch this video.");
        }
    }

    private String formatDuration(Long durationSeconds) {
        if (durationSeconds == null || durationSeconds <= 0) {
            return "00:00";
        }
        long minutes = durationSeconds / 60;
        long seconds = durationSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
