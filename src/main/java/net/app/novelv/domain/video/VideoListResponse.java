package net.app.novelv.domain.video;

public record VideoListResponse(
        Long id,
        String title,
        String channel,
        String time,
        String badge,
        String objectKey,
        String videoUrl
) {
}
