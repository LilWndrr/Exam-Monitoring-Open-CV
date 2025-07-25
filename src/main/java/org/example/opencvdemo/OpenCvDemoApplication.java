package org.example.opencvdemo;

import jakarta.annotation.PostConstruct;
import org.opencv.videoio.VideoCapture;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.opencv.core.Core;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OpenCvDemoApplication {

    static {
        System.load("C:\\Users\\Seyfullah\\Documents\\OpenCv\\opencv\\build\\java\\x64\\opencv_java4120.dll");
        System.out.println("OpenCv Loaded Version:" + Core.VERSION);
    }

    public static void main(String[] args) {
        SpringApplication.run(OpenCvDemoApplication.class, args);


    }

    @PostConstruct
    public void init() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            System.out.println("Shutdown....");
            System.exit(0);
        }));
    }

}
