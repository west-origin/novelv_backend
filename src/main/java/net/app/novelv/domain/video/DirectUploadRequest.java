package net.app.novelv.domain.video;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record DirectUploadRequest(
        @NotBlank String title,
        @NotBlank String fileName,
        String contentType,
        @NotNull @Positive Long fileSizeBytes,
        @PositiveOrZero Long durationSeconds
) {
}
