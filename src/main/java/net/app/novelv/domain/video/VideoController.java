package net.app.novelv.domain.video;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class VideoController {

    private final VideoService videoService;

    @GetMapping("/videos")
    public List<VideoListResponse> findReadyVideos() {
        return videoService.findReadyVideos(null);
    }

    @PostMapping("/creator/videos/direct-upload")
    public DirectUploadResponse createDirectUpload(
            @Valid @RequestBody DirectUploadRequest request,
            Authentication authentication
    ) {
        return videoService.createDirectUpload(request, currentUserId(authentication));
    }

    @PostMapping("/creator/videos/{videoId}/thumbnail-upload")
    public R2DirectUpload createThumbnailUpload(
            @PathVariable Long videoId,
            @Valid @RequestBody ThumbnailUploadRequest request,
            Authentication authentication
    ) {
        return videoService.createThumbnailUpload(videoId, currentUserId(authentication), request);
    }

    @PostMapping("/creator/videos/{videoId}/complete")
    public void completeUpload(
            @PathVariable Long videoId,
            @Valid @RequestBody(required = false) CompleteUploadRequest request,
            Authentication authentication
    ) {
        videoService.completeUpload(videoId, currentUserId(authentication), request);
    }

    @PostMapping("/creator/videos/{videoId}/cancel")
    public void cancelUpload(
            @PathVariable Long videoId,
            Authentication authentication
    ) {
        videoService.cancelUpload(videoId, currentUserId(authentication));
    }

    @GetMapping("/videos/{videoId}/playback-token")
    public PlaybackTokenResponse createPlaybackToken(
            @PathVariable Long videoId,
            Authentication authentication
    ) {
        return videoService.createPlaybackToken(videoId, currentUserId(authentication));
    }

    private Long currentUserId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
