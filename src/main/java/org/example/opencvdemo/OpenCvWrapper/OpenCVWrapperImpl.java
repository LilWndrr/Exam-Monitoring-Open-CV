package org.example.opencvdemo.OpenCvWrapper;

import org.example.opencvdemo.helper.SnapshotResult;
import org.example.opencvdemo.OpenCvWrapper.OpenCVWrapper;
import org.opencv.core.*;
import org.opencv.face.LBPHFaceRecognizer;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.utils.Converters;
import org.opencv.videoio.VideoCapture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class OpenCVWrapperImpl implements OpenCVWrapper {

    private final CascadeClassifier faceClassifier;
    private final CascadeClassifier sideEyeClassifier;
    private final CascadeClassifier smileClassifier;
    private final CascadeClassifier upperBodyClassifier;
    private final CascadeClassifier lowerBodyClassifier;

    // Use Spring's @Value to inject configuration
    private String frontalFaceXmlFile;

    private String profileFaceXmlFile;

    private String smileXmlFile;

    private String upperBodyXmlFile;

    private String lowerBodyXmlFile;

    @Value("${app.images.base-dir:images}")
    private String baseImagesDir;

    @Value("${opencv.model.path:trained_model.xml}")
    private String modelPath;

    public OpenCVWrapperImpl() {
        // Initialize with default paths, will be overridden by @Value
        this.frontalFaceXmlFile = "data/haarcascades/haarcascade_frontalface_alt.xml";
        this.profileFaceXmlFile = "data/haarcascades/haarcascade_profileface.xml";
        this.smileXmlFile = "data/haarcascades/haarcascade_smile.xml";
        this.upperBodyXmlFile = "data/haarcascades/haarcascade_upperbody.xml";
        this.lowerBodyXmlFile = "data/haarcascades/haarcascade_lowerbody.xml";

        this.faceClassifier = new CascadeClassifier(frontalFaceXmlFile);
        this.sideEyeClassifier = new CascadeClassifier(profileFaceXmlFile);
        this.smileClassifier = new CascadeClassifier(smileXmlFile);
        this.upperBodyClassifier = new CascadeClassifier(upperBodyXmlFile);
        this.lowerBodyClassifier = new CascadeClassifier(lowerBodyXmlFile);

        validateClassifiers();
    }

    private void validateClassifiers() {
        System.out.println("frontalFaceClassifier loaded: " + !faceClassifier.empty());
        System.out.println("profileFaceClassifier loaded: " + !sideEyeClassifier.empty());
        System.out.println("smileClassifier loaded: " + !smileClassifier.empty());
        System.out.println("upperBodyClassifier loaded: " + !upperBodyClassifier.empty());
        System.out.println("lowerBodyClassifier loaded: " + !lowerBodyClassifier.empty());
    }

    @Override
    public SnapshotResult captureAndAnalyzeSnapshot(String courseId, String userId) {
        VideoCapture videoCapture = new VideoCapture(0);

        try {
            if (!videoCapture.isOpened()) {
                return SnapshotResult.failure("Camera is not opened");
            }

            Mat frame = new Mat();
            boolean frameRead = videoCapture.read(frame);

            if (!frameRead || frame.empty()) {
                return SnapshotResult.failure("Snapshot taking failed - empty frame");
            }

            // Create file path
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path dirPath = Paths.get(baseImagesDir, courseId, userId);

            try {
                Files.createDirectories(dirPath);
            } catch (IOException e) {
                return SnapshotResult.failure("Failed to create directories: " + e.getMessage());
            }

            String fileName = timestamp + ".jpg";
            String filePath = dirPath.resolve(fileName).toString();

            // Analyze the frame
            boolean faceValid = detectFace(frame);
            boolean faceIdentityValid = checkFaceIdentity(frame);
            boolean eyeValid = detectSideEye(frame);

            // Overall status - all checks must pass
            boolean overallStatus = faceValid && eyeValid;

            // Save the image
            boolean imageSaved = Imgcodecs.imwrite(filePath, frame);
            if (!imageSaved) {
                return SnapshotResult.failure("Failed to save image");
            }

            return new SnapshotResult(filePath, overallStatus, faceIdentityValid);

        } finally {
            videoCapture.release();
            System.out.println("Camera released.");
        }
    }

    @Override
    public boolean detectFace(Mat frame) {
        if (frame.empty()) return false;

        Mat grayFrame = new Mat();
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);

        MatOfRect faceDetections = new MatOfRect();
        faceClassifier.detectMultiScale(grayFrame, faceDetections);

        int faceCount = faceDetections.toArray().length;
        System.out.println("Faces detected: " + faceCount);

        // Mark faces with rectangles if not exactly one face
        if (faceCount != 1) {
            for (Rect rect : faceDetections.toArray()) {
                Imgproc.rectangle(frame, new Point(rect.x, rect.y),
                        new Point(rect.x + rect.width, rect.y + rect.height),
                        new Scalar(0, 0, 255), 3);
            }
            return false;
        }

        return true;
    }

    @Override
    public boolean checkFaceIdentity(Mat frame) {
        if (frame.empty()) return false;

        try {
            LBPHFaceRecognizer recognizer = LBPHFaceRecognizer.create();

            // Check if model file exists
            File modelFile = new File(modelPath);
            if (!modelFile.exists()) {
                System.out.println("Model file not found: " + modelPath);
                return false;
            }

            recognizer.read(modelPath);

            Mat grayFrame = new Mat();
            Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);

            MatOfRect faceDetections = new MatOfRect();
            faceClassifier.detectMultiScale(grayFrame, faceDetections);

            Rect[] faces = faceDetections.toArray();
            if (faces.length == 0) {
                System.out.println("No faces detected for identity check");
                return false;
            }

            // Use the first detected face
            Mat face = new Mat(grayFrame, faces[0]);
            Imgproc.resize(face, face, new Size(200, 200));

            int[] label = new int[1];
            double[] confidence = new double[1];

            recognizer.predict(face, label, confidence);
            System.out.println("Predicted Label: " + label[0]);
            System.out.println("Confidence: " + confidence[0]);

            // Lower confidence means better match
            if (confidence[0] > 80) {
                System.out.println("Match not found - confidence too low");
                return false;
            } else {
                System.out.println("Match found");
                return true;
            }

        } catch (Exception e) {
            System.err.println("Error during face recognition: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean detectSideEye(Mat frame) {
        if (frame.empty()) return false;

        MatOfRect sideEyeDetections = new MatOfRect();
        int numOfEyes = 0;

        // Check original orientation
        sideEyeClassifier.detectMultiScale(frame, sideEyeDetections);
        numOfEyes += sideEyeDetections.toArray().length;

        if (numOfEyes > 0) {
            markSideEyes(frame, sideEyeDetections);
            return false;
        }

        // Check flipped orientation
        Mat flippedFrame = new Mat();
        Core.flip(frame, flippedFrame, 1);
        MatOfRect flippedDetections = new MatOfRect();
        sideEyeClassifier.detectMultiScale(flippedFrame, flippedDetections);
        numOfEyes += flippedDetections.toArray().length;

        if (numOfEyes > 0) {
            markSideEyes(flippedFrame, flippedDetections);
            return false;
        }

        System.out.println("Side eye detected: " + numOfEyes);
        return true;
    }

    private void markSideEyes(Mat frame, MatOfRect detections) {
        for (Rect rect : detections.toArray()) {
            Imgproc.rectangle(frame,
                    new Point(rect.x + 15, rect.y + 15),
                    new Point(rect.x + (rect.width - 40), rect.y + (rect.height - 40)),
                    new Scalar(120, 233, 255), 2);
        }
    }

    @Override
    public void trainModel() {
        List<Mat> images = new ArrayList<>();
        List<Integer> labels = new ArrayList<>();

        File baseDir = new File("faces");
        if (!baseDir.exists()) {
            System.err.println("Training directory 'faces' not found");
            return;
        }

        File[] userDirs = baseDir.listFiles();
        if (userDirs == null) {
            System.err.println("No user directories found in 'faces'");
            return;
        }

        for (File userDir : userDirs) {
            if (!userDir.isDirectory()) continue;

            try {
                int label = Integer.parseInt(userDir.getName());
                File[] imageFiles = userDir.listFiles();

                if (imageFiles == null) continue;

                for (File imgFile : imageFiles) {
                    if (!isImageFile(imgFile.getName())) continue;

                    Mat img = Imgcodecs.imread(imgFile.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
                    if (img.empty()) {
                        System.err.println("Could not load image: " + imgFile.getAbsolutePath());
                        continue;
                    }

                    MatOfRect faces = new MatOfRect();
                    faceClassifier.detectMultiScale(img, faces);

                    Rect[] rects = faces.toArray();
                    if (rects.length > 0) {
                        Mat face = new Mat(img, rects[0]);
                        Imgproc.resize(face, face, new Size(200, 200));
                        images.add(face);
                        labels.add(label);
                    }
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid directory name (should be numeric): " + userDir.getName());
            }
        }

        if (images.isEmpty()) {
            System.err.println("No training images found");
            return;
        }

        LBPHFaceRecognizer faceRecognizer = LBPHFaceRecognizer.create();
        faceRecognizer.train(images, Converters.vector_int_to_Mat(labels));
        faceRecognizer.write(modelPath);

        System.out.println("Training completed with " + images.size() + " images and model saved to: " + modelPath);
    }

    private boolean isImageFile(String fileName) {
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                lowerName.endsWith(".png") || lowerName.endsWith(".bmp");
    }
}
