package net.app.novelv.domain.video;

public record DirectUploadResponse(
        Long videoId,
        String objectKey,
        String uploadUrl,
        String expiresAt
) {
}
