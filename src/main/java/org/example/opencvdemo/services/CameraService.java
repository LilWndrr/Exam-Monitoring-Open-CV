package org.example.opencvdemo.services;

import jakarta.annotation.PostConstruct;
import org.apache.catalina.LifecycleState;
import org.example.opencvdemo.entity.Snapshot;
import org.example.opencvdemo.entity.User;
import org.example.opencvdemo.repository.SnapshotRepo;
import org.example.opencvdemo.repository.UserRepo;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Timestamp;
import java.sql.Time;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
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

    public List<Snapshot> getByUserIdAndExamId(Long userId, String examId) {

        return snapshotRepo.findByUserIdAndCourseId(userId, examId);
    }



    public List<Snapshot> getAll(){
        return snapshotRepo.findAll();
    }

    public List<Snapshot> getByUserId(Long userId){
        return snapshotRepo.findByUserId(userId);
    }

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
    public boolean   faceDetect(Mat frame) {
        MatOfRect faceDetections = new MatOfRect(); //Face detection started
        faceClassifier.detectMultiScale(frame, faceDetections);

        int facenum = faceDetections.toArray().length;
        System.out.println("Faces detected: " + facenum);
        //If number of faces is bigger than 1 or no one appeared in the frame marks it as suspicious
        if(facenum!=1) {
            for (Rect rect : faceDetections.toArray()) {
                Imgproc.rectangle(frame, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 255), 3);
            }
            return false;
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
