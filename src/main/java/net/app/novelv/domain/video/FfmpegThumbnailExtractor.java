package net.app.novelv.domain.video;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class FfmpegThumbnailExtractor {

    private static final Logger log = LoggerFactory.getLogger(FfmpegThumbnailExtractor.class);
    private static final String THUMBNAIL_CONTENT_TYPE = "image/webp";

    private final FfmpegProperties properties;

    public Optional<ExtractedThumbnail> extractFirstFrame(String inputUrl, Long videoId) {
        Path outputFile = null;
        try {
            outputFile = Files.createTempFile("novelv-video-" + videoId + "-", ".webp");
            Process process = new ProcessBuilder(List.of(
                    properties.resolvedPath(),
                    "-y",
                    "-hide_banner",
                    "-loglevel",
                    "error",
                    "-ss",
                    "0",
                    "-i",
                    inputUrl,
                    "-frames:v",
                    "1",
                    "-vf",
                    "scale='min(1280,iw)':-2",
                    "-c:v",
                    "libwebp",
                    "-quality",
                    "82",
                    outputFile.toString()
            )).redirectErrorStream(true).start();

            boolean finished = process.waitFor(properties.resolvedTimeoutSeconds(), TimeUnit.SECONDS);
            byte[] output = process.getInputStream().readAllBytes();
            if (!finished) {
                process.destroyForcibly();
                log.warn("FFmpeg thumbnail extraction timed out. videoId={}, timeout={}",
                        videoId, Duration.ofSeconds(properties.resolvedTimeoutSeconds()));
                deleteQuietly(outputFile);
                return Optional.empty();
            }
            if (process.exitValue() != 0 || Files.size(outputFile) == 0) {
                log.warn("FFmpeg thumbnail extraction failed. videoId={}, exitCode={}, output={}",
                        videoId, process.exitValue(), new String(output, StandardCharsets.UTF_8));
                deleteQuietly(outputFile);
                return Optional.empty();
            }

            return Optional.of(new ExtractedThumbnail(outputFile, THUMBNAIL_CONTENT_TYPE));
        } catch (IOException exception) {
            log.warn("FFmpeg thumbnail extraction IO failure. videoId={}", videoId, exception);
            deleteQuietly(outputFile);
            return Optional.empty();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("FFmpeg thumbnail extraction interrupted. videoId={}", videoId, exception);
            deleteQuietly(outputFile);
            return Optional.empty();
        }
    }

    private void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            log.debug("Failed to delete temporary thumbnail file. path={}", file, exception);
        }
    }

    public record ExtractedThumbnail(Path file, String contentType) {
    }
}
