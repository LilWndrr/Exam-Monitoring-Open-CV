package org.example.opencvdemo.OpenCvWrapper;

import org.example.opencvdemo.helper.SnapshotResult;
import org.opencv.core.Mat;

public interface OpenCVWrapper {
    SnapshotResult captureAndAnalyzeSnapshot(String courseId, String userId);
    boolean detectFace(Mat frame);
    boolean checkFaceIdentity(Mat frame);
    boolean detectSideEye(Mat frame);
    void trainModel();
}