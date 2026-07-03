package net.app.novelv.domain.video;

public record R2DirectUpload(
        String objectKey,
        String uploadUrl,
        String expiresAt
) {
}
