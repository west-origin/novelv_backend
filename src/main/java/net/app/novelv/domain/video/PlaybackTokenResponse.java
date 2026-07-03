package net.app.novelv.domain.video;

public record PlaybackTokenResponse(
        Long videoId,
        String objectKey,
        String playbackUrl,
        long expiresInSeconds
) {
}
