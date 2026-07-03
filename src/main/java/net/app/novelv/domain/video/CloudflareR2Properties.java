package net.app.novelv.domain.video;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cloudflare.r2")
public record CloudflareR2Properties(
        String accountId,
        String accessKeyId,
        String secretAccessKey,
        String bucketName,
        String endpoint,
        long uploadUrlTtlSeconds,
        long playbackUrlTtlSeconds,
        Boolean cleanupEnabled,
        Long cleanupGraceMinutes,
        Integer cleanupBatchSize
) {
    public String resolvedEndpoint() {
        if (endpoint != null && !endpoint.isBlank()) {
            return endpoint;
        }
        return "https://" + accountId + ".r2.cloudflarestorage.com";
    }

    public long resolvedUploadUrlTtlSeconds() {
        return uploadUrlTtlSeconds > 0 ? uploadUrlTtlSeconds : 900;
    }

    public long resolvedPlaybackUrlTtlSeconds() {
        return playbackUrlTtlSeconds > 0 ? playbackUrlTtlSeconds : 300;
    }

    public boolean resolvedCleanupEnabled() {
        return cleanupEnabled == null || cleanupEnabled;
    }

    public long resolvedCleanupGraceMinutes() {
        return cleanupGraceMinutes != null && cleanupGraceMinutes > 0 ? cleanupGraceMinutes : 60;
    }

    public int resolvedCleanupBatchSize() {
        return cleanupBatchSize != null && cleanupBatchSize > 0 ? cleanupBatchSize : 50;
    }
}