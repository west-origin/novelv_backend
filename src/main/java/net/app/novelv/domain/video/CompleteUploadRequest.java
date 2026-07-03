package net.app.novelv.domain.video;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CompleteUploadRequest(
        @Size(max = 200) String title,
        @PositiveOrZero Long durationSeconds
) {
}
