package org.example.opencvdemo;

import jakarta.annotation.PostConstruct;
import org.opencv.videoio.VideoCapture;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.opencv.core.Core;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class OpenCvDemoApplication {

    private static boolean isOpenCvLoaded = false;

    static {
        if (!isOpenCvLoaded) {
            try {
                System.load("C:\\Users\\Seyfullah\\Documents\\openCV1\\install\\java\\opencv_java4120.dll");
                isOpenCvLoaded = true;
            } catch (UnsatisfiedLinkError e) {
                if (!e.getMessage().contains("already loaded")) {
                    throw e;
                }
            }
        }
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
