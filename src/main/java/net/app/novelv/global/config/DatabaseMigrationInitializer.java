package net.app.novelv.global.config;

import lombok.RequiredArgsConstructor;
import net.app.novelv.domain.video.VideoStatus;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DatabaseMigrationInitializer implements ApplicationRunner {

    private static final String VIDEOS_STATUS_CONSTRAINT = "chk_videos_status";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        migrateVideosTableForR2();
    }

    private void migrateVideosTableForR2() {
        jdbcTemplate.execute("ALTER TABLE videos ADD COLUMN IF NOT EXISTS r2_object_key VARCHAR(1024)");
        jdbcTemplate.execute("ALTER TABLE videos ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP");
        jdbcTemplate.execute("ALTER TABLE videos ALTER COLUMN cloudflare_video_id DROP NOT NULL");
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_videos_r2_object_key ON videos (r2_object_key) WHERE r2_object_key IS NOT NULL");
        recreateVideosStatusConstraint();
    }

    private void recreateVideosStatusConstraint() {
        jdbcTemplate.execute("ALTER TABLE videos DROP CONSTRAINT IF EXISTS videos_status_check");
        jdbcTemplate.execute("ALTER TABLE videos DROP CONSTRAINT IF EXISTS " + VIDEOS_STATUS_CONSTRAINT);
        jdbcTemplate.execute("""
                ALTER TABLE videos
                ADD CONSTRAINT %s
                CHECK (status IN (%s))
                """.formatted(VIDEOS_STATUS_CONSTRAINT, videoStatusSqlValues()));
    }

    private String videoStatusSqlValues() {
        return Arrays.stream(VideoStatus.values())
                .map(status -> "'" + status.name() + "'")
                .collect(Collectors.joining(", "));
    }
}
