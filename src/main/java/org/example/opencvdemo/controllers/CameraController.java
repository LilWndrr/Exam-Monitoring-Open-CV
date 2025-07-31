package org.example.opencvdemo.controllers;

import org.example.opencvdemo.entity.Snapshot;
import org.example.opencvdemo.repository.SnapshotRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.example.opencvdemo.services.CameraService;

import java.awt.geom.RectangularShape;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

@RestController
@EnableScheduling

public class CameraController {
    private final CameraService cameraService;
    private final TaskScheduler taskScheduler;
    private final SnapshotRepo snapshotRepo;

    private ScheduledFuture<?> scheduledTask;

    private volatile boolean isRunning = false;

    public CameraController(CameraService cameraService, TaskScheduler taskScheduler, SnapshotRepo snapshotRepo) {
        this.cameraService = cameraService;
        this.taskScheduler = taskScheduler;
        this.snapshotRepo = snapshotRepo;
    }


    @GetMapping("admin/getAll")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public  ResponseEntity<List<Snapshot>> getAll(){
        return ResponseEntity.ok(cameraService.getAll());
    }

    @GetMapping("admin/getByUserId")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")

    public ResponseEntity<List<Snapshot>> getByUserId(Long userId){
        return ResponseEntity.ok(cameraService.getByUserId(userId));
    }


    @GetMapping("admin/getByUserIdAndExamAd")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<Snapshot>> getByUserIdAndExamAd(Long userId, Long examAdId){
        return ResponseEntity.ok(cameraService.getByUserIdAndExamId(userId,examAdId.toString()));
    }




    @GetMapping("user/capture")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<String> capture(Long examId){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        String result= cameraService.takeSnapshot(username, examId);
        return ResponseEntity.ok(result);
    }



    @GetMapping("user/startScheduler")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<String> startCapturing(Long examId){
        if(isRunning){
            return ResponseEntity.ok("Is already running");
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        scheduledTask = taskScheduler.scheduleAtFixedRate(
                ()-> cameraService.takeSnapshot(username,examId),1000
        );

        isRunning = true;
        return ResponseEntity.ok("Started");
    }

    @GetMapping("user/stopCapturing")
    @PreAuthorize("hasAuthority('ROLE_USER')")

    public ResponseEntity<String> stopCapturing(){
        if(!isRunning || scheduledTask == null){
            return ResponseEntity.ok("Is not running");
        }

        scheduledTask.cancel(false);
        isRunning = false;
        return ResponseEntity.ok("Stopped");
    }

}
