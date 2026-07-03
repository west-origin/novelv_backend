package net.app.novelv.domain.video;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VideoUploadCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(VideoUploadCleanupScheduler.class);

    private final CloudflareR2Properties properties;
    private final VideoService videoService;

    @Scheduled(
            initialDelayString = "${app.cloudflare.r2.cleanup-initial-delay-ms:300000}",
            fixedDelayString = "${app.cloudflare.r2.cleanup-fixed-delay-ms:3600000}"
    )
    public void cleanupAbandonedUploads() {
        if (!properties.resolvedCleanupEnabled()) {
            return;
        }

        int cleaned = videoService.cleanupAbandonedUploads();
        if (cleaned > 0) {
            log.info("Cleaned {} abandoned video upload(s).", cleaned);
        }
    }
}