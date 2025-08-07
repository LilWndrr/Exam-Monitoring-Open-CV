package org.example.opencvdemo.repository;

import org.assertj.core.api.Assertions;
import org.example.opencvdemo.entity.Snapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class SnapshotRepositoryTest {
    @Autowired
    private SnapshotRepo snapshotRepo;

    @Test
    public void SnapshotRepository_SaveAll_ReturnsSavedSnapshot() {
        Snapshot snapshot = Snapshot.builder().courseId("2").faceIdentity(true).status(true).filePath("some/file/path").build();



       Snapshot savedSnapshot= snapshotRepo.save(snapshot);

        Assertions.assertThat(savedSnapshot).isNotNull();
        Assertions.assertThat(savedSnapshot.getUUID().length()).isEqualTo(36);
    }
    @Test
    public void SnapshotRepository_GetAll_ReturnsMoreThanOneSnapshot() {
        Snapshot snapshot1 = Snapshot.builder().courseId("2").faceIdentity(true).status(true).filePath("some/file/path").build();
        Snapshot snapshot2 = Snapshot.builder().courseId("2").faceIdentity(true).status(true).filePath("some/file/path").build();
        Snapshot snapshot3 = Snapshot.builder().courseId("2").faceIdentity(true).status(true).filePath("some/file/path").build();



        Snapshot savedSnapshot1= snapshotRepo.save(snapshot1);
        Snapshot savedSnapshot2= snapshotRepo.save(snapshot2);
        Snapshot savedSnapshot3= snapshotRepo.save(snapshot3);

        List<Snapshot> snapshots = snapshotRepo.findAll();

        Assertions.assertThat(snapshots).isNotNull();
        Assertions.assertThat(snapshots.size()).isEqualTo(3);
    }
    @Test
    public void SnapshotRepository_FindById_ReturnsSnapshot() {
        Snapshot snapshot1 = Snapshot.builder().courseId("2").faceIdentity(true).status(true).filePath("some/file/path").build();


        snapshotRepo.save(snapshot1);

        Snapshot savedSnapshot1= snapshotRepo.findById(snapshot1.getUUID()).orElse(null);


        Assertions.assertThat(savedSnapshot1).isNotNull();
        Assertions.assertThat(savedSnapshot1.getUUID()).isEqualTo(snapshot1.getUUID());
    }

    @Test
    public void SnapshotRepository_Update_ReturnsSnapshot() {
        Snapshot snapshot1 = Snapshot.builder().courseId("2").faceIdentity(true).status(true).filePath("some/file/path").build();


        snapshotRepo.save(snapshot1);
        snapshotRepo.delete(snapshot1);


        Snapshot deletedSnapshot1= snapshotRepo.findById(snapshot1.getUUID()).orElse(null);



        Assertions.assertThat(deletedSnapshot1).isNull();

    }

    @Test
    public void SnapshotRepository_Delete_ReturnsNothing()
    {
        Snapshot snapshot1 = Snapshot.builder().courseId("2").faceIdentity(true).status(true).filePath("some/file/path").build();


        snapshotRepo.save(snapshot1);

        Snapshot savedSnapshot1= snapshotRepo.findById(snapshot1.getUUID()).orElse(null);
        savedSnapshot1.setFilePath("some/file/path");
        savedSnapshot1.setStatus(false);
        Snapshot updateSnapshot= snapshotRepo.save(savedSnapshot1);


        Assertions.assertThat(updateSnapshot.getFilePath()).isNotNull();
        Assertions.assertThat(updateSnapshot.getStatus()).isEqualTo(false);
    }
}
