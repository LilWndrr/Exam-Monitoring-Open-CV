package org.example.opencvdemo.services;

import org.example.opencvdemo.entity.Snapshot;
import org.example.opencvdemo.entity.User;
import org.example.opencvdemo.exception.ApiRequestException;
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
    private final CascadeClassifier faceClassifier;
    private final CascadeClassifier sideEyeClassifier;
    private final CascadeClassifier smileClassifier;

    private final CascadeClassifier upperBodyClassifier;
    private final CascadeClassifier lowerBodyClassifier;

    public CameraService(SnapshotRepo snapshotRepo, UserRepo userRepo) {
        this.snapshotRepo = snapshotRepo;
        this.userRepo = userRepo;

        String frontalFaceXmlFile = "C:\\Users\\Seyfullah\\Desktop\\Java\\OpenCvDemo\\data\\haarcascades\\haarcascade_frontalface_alt.xml";
        String profileFaceXmlFile = "C:\\Users\\Seyfullah\\Desktop\\Java\\OpenCvDemo\\data\\haarcascades\\haarcascade_profileface.xml";
        String smileXmlFile = "C:\\Users\\Seyfullah\\Desktop\\Java\\OpenCvDemo\\data\\haarcascades\\haarcascade_smile.xml";
        String upperBodyXmlFile = "C:\\Users\\Seyfullah\\Desktop\\Java\\OpenCvDemo\\data\\haarcascades\\haarcascade_upperbody.xml";
        String lowerBodyXmlFile = "C:\\Users\\Seyfullah\\Desktop\\Java\\OpenCvDemo\\data\\haarcascades\\haarcascade_lowerbody.xml";


        this.faceClassifier = new CascadeClassifier(frontalFaceXmlFile);
        this.sideEyeClassifier = new CascadeClassifier(profileFaceXmlFile);
        this.smileClassifier = new CascadeClassifier(smileXmlFile);
        this.upperBodyClassifier = new CascadeClassifier(upperBodyXmlFile);
        this.lowerBodyClassifier = new CascadeClassifier(lowerBodyXmlFile);
        System.out.println("frontalFaceClassifier loaded: " + !faceClassifier.empty());
        System.out.println("profileFaceClassifier loaded: " + !sideEyeClassifier.empty());
        System.out.println("smileClassifier loaded: " + !smileClassifier.empty());
        System.out.println("upperBodyClassifier loaded: " + !upperBodyClassifier.empty());
        System.out.println("lowerBodyClassifier loaded: " + !lowerBodyClassifier.empty());
    }


    @Cacheable(value = "snapshots", key = "#userId + '-' + #examId")
    public List<Snapshot> getByUserIdAndExamId(Long userId, String examId) {
        System.out.println(">> Hitting the database...");
        return snapshotRepo.findByUserIdAndCourseId(userId, examId);
    }


    @Cacheable(value = "allSnapshots")
    public List<Snapshot> getAll(){
        return snapshotRepo.findAll();
    }

    public List<Snapshot> getByUserId(Long userId){
        List<Snapshot> snapshots = snapshotRepo.findByUserId(userId);
        if(snapshots.isEmpty()){
            throw new ApiRequestException("No snapshots found or invalid UserId");
        }
        return snapshotRepo.findByUserId(userId);
    }

    public void train (){
            List<Mat> images = new ArrayList<>();
            List<Integer> labels = new ArrayList<>();

        File baseDir = new File("faces");

        for(File file : baseDir.listFiles()){
            int  label = Integer.parseInt(file.getName());

            for(File imgFile : file.listFiles()){
                Mat img = Imgcodecs.imread(imgFile.getAbsolutePath(),Imgcodecs.IMREAD_GRAYSCALE);
                MatOfRect faces = new MatOfRect();
                faceClassifier.detectMultiScale(img, faces);

                Rect[] rects = faces.toArray();

                if(rects.length > 0){
                    Mat face = new Mat(img, rects[0]);
                    Imgproc.resize(face,face,new Size(200,200));
                    images.add(face);
                    labels.add(label);
                }


            }
        }

        LBPHFaceRecognizer faceRecognizer= LBPHFaceRecognizer.create();
        faceRecognizer.train(images, Converters.vector_int_to_Mat(labels));
        faceRecognizer.write("trained_model.xml");

        System.out.println("Training completed and model saved.");

    }


    @CacheEvict(value = "allSnapshots",allEntries = true)
    public String takeSnapshot(String username,Long examId) {
        System.out.println("Starting snapshot...");
        VideoCapture videoCapture = new VideoCapture(0);// Staring of camera usage
        User user = userRepo.findByUsername(username);
        String userId = user.getId().toString();
        String courseId = examId.toString();



        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        String baseDir ="images";
        Path dirPath = Paths.get(baseDir, courseId, userId);

        try {
            Files.createDirectories(dirPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String fileName = timestamp + ".jpg";
        String filePath = dirPath.resolve(fileName).toString();


        try {
            if (!videoCapture.isOpened()) {
                return "Camera is not opened";
            }


            Mat frame = new Mat();
            videoCapture.read(frame); // Catch the frame (snapshot taking)

            if (frame.empty()) {
                return "Snapshot taking failed";
            }
            Snapshot snapshot = new Snapshot();

            snapshot.setUser(user);
            snapshot.setCourseId(courseId);
            snapshot.setFilePath(filePath);
            snapshot.setStatus(true);

            Mat faceDetected = frame;
            snapshot.setStatus(faceDetect(faceDetected));

            snapshot.setFaceIdentity(checkFaceIdentity(frame));


            Mat sideEyeDetected = frame;
            snapshot.setStatus(eyeDetect(sideEyeDetected));

            Imgcodecs.imwrite(filePath, frame);


            snapshotRepo.save(snapshot);
            //Smile detection haarcascade is not working properly
          /*  Mat smileDetected = frame.clone();
            int numOfSmiles = smileDetect(smileDetected);
            if(numOfSmiles>0){
                Imgcodecs.imwrite("smileDetected.jpg", smileDetected);
            }*/


            /*Mat upperBodyDetected = frame.clone();
            int numOfUpperBody = upperBodyDetect(upperBodyDetected);
            if(numOfUpperBody==1){
                Imgcodecs.imwrite("upperBodyDetected.jpg", upperBodyDetected);
            }

            Mat lowerBodyDetected = frame.clone();
            int numOfLowerBody = lowerBodyDetect(lowerBodyDetected);
            if(numOfLowerBody>0){
                Imgcodecs.imwrite("lowerBodyDetected.jpg", lowerBodyDetected);
            }*/
            //Creates Demo Snapshot if any suspicious action was caught
            //Imgcodecs.imwrite("demoSnapshot.jpg", frame);
            return "Snapshot taken successfully";

        } finally {
            videoCapture.release();
            System.out.println("Camera released.");
        }
    }


    //Detection of face numbers in the frame
    public boolean faceDetect(Mat frame) {
        LBPHFaceRecognizer recognizer = LBPHFaceRecognizer.create();
        recognizer.read("trained_model.xml");

        // Convert frame to grayscale for face detection and recognition
        Mat grayFrame = new Mat();
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);

        MatOfRect faceDetections = new MatOfRect();
        faceClassifier.detectMultiScale(grayFrame, faceDetections); // Use grayscale frame

        int facenum = faceDetections.toArray().length;
        System.out.println("Faces detected: " + facenum);

        // If number of faces is bigger than 1 or no one appeared in the frame marks it as suspicious
        if(facenum != 1) {
            for (Rect rect : faceDetections.toArray()) {
                Imgproc.rectangle(frame, new Point(rect.x, rect.y),
                        new Point(rect.x + rect.width, rect.y + rect.height),
                        new Scalar(0, 0, 255), 3);
            }
            return false;
        }
        // Extract face from grayscale image for recognition
        Mat face = new Mat(grayFrame, faceDetections.toArray()[0]); // Use grayscale frame
        Imgproc.resize(face, face, new Size(200, 200));

        int[] label = new int[1];
        double[] confidence = new double[1];

        recognizer.predict(face, label, confidence); // Now using grayscale face
        System.out.println("Predicted Label: " + label[0]);
        System.out.println("Confidence: " + confidence[0]);

        if (confidence[0] >70) {

            System.out.println("Match not found: User ID ");
            return false;
        } else {
            System.out.println("Match found");
        }

        return true;
    }


    public boolean checkFaceIdentity(Mat frame) {
        LBPHFaceRecognizer recognizer = LBPHFaceRecognizer.create();
        recognizer.read("trained_model.xml");

        // Convert frame to grayscale for face detection and recognition
        Mat grayFrame = new Mat();
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);

        MatOfRect faceDetections = new MatOfRect();
        faceClassifier.detectMultiScale(grayFrame, faceDetections); // Use grayscale frame

        // Extract face from grayscale image for recognition
        Mat face = new Mat(grayFrame, faceDetections.toArray()[0]); // Use grayscale frame
        Imgproc.resize(face, face, new Size(200, 200));

        int[] label = new int[1];
        double[] confidence = new double[1];

        recognizer.predict(face, label, confidence); // Now using grayscale face
        System.out.println("Predicted Label: " + label[0]);
        System.out.println("Confidence: " + confidence[0]);

        if (confidence[0] >80) {

            System.out.println("Match not found: User ID ");
            return false;
        } else {
            System.out.println("Match found");
        }

        return true;
    }


    // Detection of suspicious glances outside the monitor
    public boolean eyeDetect(Mat frame) {
        MatOfRect sideEyeDetections = new MatOfRect();
        int numOfEyes=0;

        sideEyeClassifier.detectMultiScale(frame, sideEyeDetections);
        numOfEyes += sideEyeDetections.toArray().length;
        if(numOfEyes>0) {
            for (Rect rect : sideEyeDetections.toArray()) {
                Imgproc.rectangle(frame, new Point(rect.x+50, rect.y+50),  new Point(rect.x + (rect.width-100), rect.y + (rect.height-100)), new Scalar(120, 233, 255), 2);
            }
            return false;
        }
        Core.flip(frame, frame, +1);
        sideEyeClassifier.detectMultiScale(frame, sideEyeDetections);
        numOfEyes += sideEyeDetections.toArray().length;
        if(numOfEyes>0) {
            for (Rect rect : sideEyeDetections.toArray()) {

                Imgproc.rectangle(frame, new Point(rect.x+50, rect.y+50), new Point(rect.x + (rect.width-100), rect.y + (rect.height-100)), new Scalar(120, 233, 255), 2);
            }
            return false;
        }
        System.out.println("Side eye detected: " + numOfEyes);



        return  true;
    }

    public int smileDetect(Mat frame) {
        MatOfRect smileDetections = new MatOfRect();
        smileClassifier.detectMultiScale(frame, smileDetections);
        int numOfSmiles = smileDetections.toArray().length;
        for (Rect rect : smileDetections.toArray()) {
            Imgproc.rectangle(frame, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(120, 233, 255), 2);
        }

        return  numOfSmiles;
    }

    public int upperBodyDetect(Mat frame) {
        MatOfRect upperBodyDetections = new MatOfRect();
        upperBodyClassifier.detectMultiScale(frame, upperBodyDetections);
        int numOfUpperBody = upperBodyDetections.toArray().length;
        for (Rect rect : upperBodyDetections.toArray()) {
            Imgproc.rectangle(frame, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 255), 2);

        }
        return  numOfUpperBody;
    }

    public int lowerBodyDetect(Mat frame) {
        MatOfRect lowerBodyDetections = new MatOfRect();
        lowerBodyClassifier.detectMultiScale(frame, lowerBodyDetections);
        int numOfLowerBody = lowerBodyDetections.toArray().length;
        for (Rect rect : lowerBodyDetections.toArray()) {
            Imgproc.rectangle(frame, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 255), 2);
        }
        return numOfLowerBody;
    }
}
