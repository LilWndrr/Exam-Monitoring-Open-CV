package org.example.opencvdemo.services;

import org.example.opencvdemo.OpenCvWrapper.OpenCVWrapper;
import org.example.opencvdemo.entity.Snapshot;
import org.example.opencvdemo.entity.User;
import org.example.opencvdemo.exception.ApiRequestException;
import org.example.opencvdemo.helper.SnapshotResult;
import org.example.opencvdemo.repository.SnapshotRepo;
import org.example.opencvdemo.repository.UserRepo;
import org.opencv.core.*;
import org.opencv.face.FaceRecognizer;
import org.opencv.face.LBPHFaceRecognizer;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import org.opencv.utils.Converters;
import org.opencv.videoio.VideoCapture;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service

public class CameraService {

    private final SnapshotRepo snapshotRepo;
    private final UserRepo userRepo;
    private final OpenCVWrapper openCVWrapper;

    public CameraService(SnapshotRepo snapshotRepo, UserRepo userRepo, OpenCVWrapper openCVWrapper) {
        this.snapshotRepo = snapshotRepo;
        this.userRepo = userRepo;
        this.openCVWrapper = openCVWrapper;
    }

    @Cacheable(value = "snapshots", key = "#userId + '-' + #examId")
    public List<Snapshot> getByUserIdAndExamId(Long userId, String examId) {
        System.out.println(">> Hitting the database...");
        return snapshotRepo.findByUserIdAndCourseId(userId, examId);
    }

    @Cacheable(value = "allSnapshots")
    public List<Snapshot> getAll() {
        return snapshotRepo.findAll();
    }

    public List<Snapshot> getByUserId(Long userId) {
        List<Snapshot> snapshots = snapshotRepo.findByUserId(userId);
        if (snapshots.isEmpty()) {
            throw new ApiRequestException("No snapshots found or invalid UserId");
        }
        return snapshots;
    }

    public void train() {
        openCVWrapper.trainModel();
    }

    @CacheEvict(value = "allSnapshots", allEntries = true)
    public String takeSnapshot(String username, Long examId) {
        System.out.println("Starting snapshot...");

        User user = userRepo.findByUsername(username);
        if (user == null) {
            throw new ApiRequestException("User not found: " + username);
        }

        String userId = user.getId().toString();
        String courseId = examId.toString();

        try {
            SnapshotResult result = openCVWrapper.captureAndAnalyzeSnapshot(courseId, userId);

            if (!result.isValid() && result.getFilePath() == null) {
                // Complete failure - no image was saved
                return result.getErrorMessage();
            }

            // Create and save snapshot entity
            Snapshot snapshot = Snapshot.builder()
                    .user(user)
                    .courseId(courseId)
                    .filePath(result.getFilePath())
                    .status(result.isValid())
                    .faceIdentity(result.isFaceIdentityValid())
                    .build();

            snapshotRepo.save(snapshot);

            if (result.isValid()) {
                return "Snapshot taken successfully";
            } else {
                return "Snapshot taken but suspicious activity detected";
            }

        } catch (Exception e) {
            System.err.println("Error taking snapshot: " + e.getMessage());
            return "Snapshot taking failed: " + e.getMessage();
        }
    }
}
