package net.app.novelv.domain.video;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VideoRepository extends JpaRepository<Video, Long> {

    Optional<Video> findByIdAndUploaderId(Long id, Long uploaderId);

    List<Video> findTop20ByStatusOrderByCreatedAtDesc(VideoStatus status);

    List<Video> findTop50ByStatusInAndUpdatedAtBeforeOrderByUpdatedAtAsc(
            Collection<VideoStatus> statuses,
            LocalDateTime updatedAt
    );

    @Modifying
    @Query("""
            delete from Video v
            where v.id = :id
              and v.uploaderId = :uploaderId
              and v.status in :statuses
            """)
    int deleteByIdAndUploaderIdAndStatusIn(
            @Param("id") Long id,
            @Param("uploaderId") Long uploaderId,
            @Param("statuses") Collection<VideoStatus> statuses
    );
}
