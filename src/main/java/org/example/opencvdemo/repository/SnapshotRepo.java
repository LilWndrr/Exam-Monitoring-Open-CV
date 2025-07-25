package org.example.opencvdemo.repository;

import org.example.opencvdemo.entity.Snapshot;
import org.example.opencvdemo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SnapshotRepo extends JpaRepository<Snapshot, String> {
        List<Snapshot> findByUserId(Long user_id);
        List<Snapshot> findByUserIdAndCourseId(Long user_id, String courseId);

}
