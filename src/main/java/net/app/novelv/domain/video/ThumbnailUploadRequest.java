package net.app.novelv.domain.video;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ThumbnailUploadRequest(
        @NotBlank String fileName,
        @NotBlank String contentType,
        @NotNull @Positive Long fileSizeBytes
) {
}
