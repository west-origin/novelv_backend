package net.app.novelv.domain.video;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ffmpeg")
public record FfmpegProperties(
        String path,
        Integer timeoutSeconds
) {
    public String resolvedPath() {
        return path == null || path.isBlank() ? "ffmpeg" : path;
    }

    public int resolvedTimeoutSeconds() {
        return timeoutSeconds != null && timeoutSeconds > 0 ? timeoutSeconds : 30;
    }
}
