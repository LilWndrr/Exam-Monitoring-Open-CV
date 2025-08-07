package org.example.opencvdemo.service;

import org.assertj.core.api.Assertions;
import org.example.opencvdemo.OpenCvWrapper.OpenCVWrapper;
import org.example.opencvdemo.entity.Snapshot;
import org.example.opencvdemo.entity.User;
import org.example.opencvdemo.exception.ApiRequestException;
import org.example.opencvdemo.helper.SnapshotResult;
import org.example.opencvdemo.repository.SnapshotRepo;
import org.example.opencvdemo.repository.UserRepo;
import org.example.opencvdemo.services.CameraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Mockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CameraServiceTest {
    @Mock
    private SnapshotRepo snapshotRepo;
    @Mock
    private UserRepo userRepo;

    @Mock
    private OpenCVWrapper openCVWrapper;

    @InjectMocks
    private CameraService cameraService;

    private User testUser;
    private Snapshot testSnapshot;
    private List<Snapshot> testSnapshots;
    long ExamId = 1L;


    @BeforeEach
    void setUp() {


        testUser = User.builder().id(1L)
                .username("TestUser")
                .password("password").
                snapshots(new HashSet<>())
                .roles(new HashSet<>()).build();
        testSnapshot = Snapshot.builder().user(testUser)
                .UUID("test-UUID-1")
                .courseId(Long.toString(ExamId))
                .faceIdentity(true)
                .status(true)
                .filePath("test/filepath/snapshot.jpg").build();
        testSnapshots = Arrays.asList(testSnapshot,
                Snapshot.builder().user(testUser)
                        .UUID("test-UUID-2")
                        .courseId(Long.toString(ExamId))
                        .faceIdentity(false)
                        .status(false)
                        .filePath("test/filepath/snapshot2.jpg").build());

    }

    @Test
    public void CameraServiceTest_GetByUserIdAndExamId_ReturnsSnapshots(){
        Long userId = 1L;
        String examId = Long.toString(ExamId);

        when(snapshotRepo.findByUserIdAndCourseId(userId,examId)).thenReturn(testSnapshots);

        List<Snapshot> snapshots = cameraService.getByUserIdAndExamId(userId,examId);

        Assertions.assertThat(snapshots).isNotEmpty();
        Assertions.assertThat(snapshots).hasSize(2);
        Assertions.assertThat("test-UUID-1").isEqualTo(snapshots.getFirst().getUUID());


    }

    @Test
    public void CameraServiceTest_GetAll_ReturnsAllSnapshots(){
        when(snapshotRepo.findAll()).thenReturn(testSnapshots);

        List<Snapshot> snapshots = cameraService.getAll();

        Assertions.assertThat(snapshots).isNotEmpty();
        Assertions.assertThat(snapshots).hasSize(2);
        Assertions.assertThat("test-UUID-1").isEqualTo(snapshots.getFirst().getUUID());
        Assertions.assertThat("test-UUID-2").isEqualTo(snapshots.getLast().getUUID());
        Mockito.verify(snapshotRepo, Mockito.times(1)).findAll();
    }

    @Test
    public void CameraServiceTest_GetByUserId_ReturnsSnapshots(){
        Long userId = 1L;

        when(snapshotRepo.findByUserId(userId)).thenReturn(testSnapshots);

        List<Snapshot> snapshots = cameraService.getByUserId(userId);

        Assertions.assertThat(snapshots).isNotEmpty();
        Assertions.assertThat(snapshots).hasSize(2);
        Assertions.assertThat("test-UUID-1").isEqualTo(snapshots.getFirst().getUUID());


    }

    @Test
    public void CameraServiceTest_Train_ReturnsNothing(){

        doNothing().when(openCVWrapper).trainModel();

        cameraService.train();

        verify(openCVWrapper).trainModel();

    }

    @Test
    public void CameraServiceTest_TakeSnapshot_ReturnsResultSuccessResponseWithoutSuspiciousActivity(){
        Long userId = 1L;
        String username = "TestUser";
        String examId = Long.toString(ExamId);

        String expectedFilePath = "/path/to/snapshot.jpg";

        SnapshotResult successResult = SnapshotResult.success(expectedFilePath,true);
        when(userRepo.findByUsername(username)).thenReturn(testUser);
        when(openCVWrapper.captureAndAnalyzeSnapshot(examId,userId.toString())).thenReturn(successResult);
        when(snapshotRepo.save(any(Snapshot.class))).thenReturn(testSnapshot);

        String result= cameraService.takeSnapshot(username,ExamId);


        Assertions.assertThat(result).isEqualTo("Snapshot taken successfully");
        verify(userRepo).findByUsername(username);
        verify(openCVWrapper).captureAndAnalyzeSnapshot(examId,testUser.getId().toString());
        verify(snapshotRepo).save(argThat(snapshot ->
                snapshot.getUser().equals(testUser)&&
                snapshot.getFilePath().equals(expectedFilePath)&&
                snapshot.getCourseId().equals(examId)&&
                snapshot.getStatus()&&
                snapshot.isFaceIdentity()));


    }
    @Test
    public void CameraServiceTest_TakeSnapshot_ReturnsResultSuccessResponseWithSuspiciousActivity(){
        Long userId = 1L;
        String username = "TestUser";
        String examId = Long.toString(ExamId);

        String expectedFilePath = "/path/to/snapshot.jpg";

        SnapshotResult successResult = new SnapshotResult(expectedFilePath,false,false);
        when(userRepo.findByUsername(username)).thenReturn(testUser);
        when(openCVWrapper.captureAndAnalyzeSnapshot(examId,userId.toString())).thenReturn(successResult);
        when(snapshotRepo.save(any(Snapshot.class))).thenReturn(testSnapshots.getLast());

        String result= cameraService.takeSnapshot(username,ExamId);


        Assertions.assertThat(result).isEqualTo("Snapshot taken but suspicious activity detected");
        verify(userRepo).findByUsername(username);
        verify(openCVWrapper).captureAndAnalyzeSnapshot(examId,testUser.getId().toString());
        verify(snapshotRepo).save(argThat(snapshot ->
                snapshot.getUser().equals(testUser)&&
                        snapshot.getFilePath().equals(expectedFilePath)&&
                        snapshot.getCourseId().equals(examId)&&
                        !snapshot.getStatus()&&
                        !snapshot.isFaceIdentity()));


    }

    @Test
    public void CameraServiceTest_TakeSnapshot_ReturnsResultFailureWhenCameraFails(){
        Long userId = 1L;
        String username = "TestUser";
        String examId = Long.toString(ExamId);



        SnapshotResult failureResult = SnapshotResult.failure("Camera is not opened");
        when(userRepo.findByUsername(username)).thenReturn(testUser);
        when(openCVWrapper.captureAndAnalyzeSnapshot(examId,userId.toString())).thenReturn(failureResult);

        String result= cameraService.takeSnapshot(username,ExamId);



        Assertions.assertThat(result).isEqualTo("Camera is not opened");
        verify(userRepo).findByUsername(username);
        verify(openCVWrapper).captureAndAnalyzeSnapshot(examId,testUser.getId().toString());
        verify(snapshotRepo, never()).save(any(Snapshot.class));


    }

    @Test
    public void CameraServiceTest_TakeSnapshot_ReturnsResultFailureWhenInvalidFrame(){
        Long userId = 1L;
        String username = "TestUser";
        String examId = Long.toString(ExamId);



        SnapshotResult failureResult = SnapshotResult.failure("Snapshot taking failed - empty frame");
        when(userRepo.findByUsername(username)).thenReturn(testUser);
        when(openCVWrapper.captureAndAnalyzeSnapshot(examId,userId.toString())).thenReturn(failureResult);

        String result= cameraService.takeSnapshot(username,ExamId);



        Assertions.assertThat(result).isEqualTo("Snapshot taking failed - empty frame");
        verify(userRepo).findByUsername(username);
        verify(openCVWrapper).captureAndAnalyzeSnapshot(examId,testUser.getId().toString());
        verify(snapshotRepo, never()).save(any(Snapshot.class));

    }

    @Test
    public void CameraServiceTest_TakeSnapshot_ReturnsResultFailureWhenDirectoryCreationFails(){
        Long userId = 1L;
        String username = "TestUser";
        String examId = Long.toString(ExamId);



        SnapshotResult failureResult = SnapshotResult.failure("Failed to create directories: some error message");
        when(userRepo.findByUsername(username)).thenReturn(testUser);
        when(openCVWrapper.captureAndAnalyzeSnapshot(examId,userId.toString())).thenReturn(failureResult);

        String result= cameraService.takeSnapshot(username,ExamId);



        Assertions.assertThat(result).matches("Failed to create directories:.*");
        verify(userRepo).findByUsername(username);
        verify(openCVWrapper).captureAndAnalyzeSnapshot(examId,testUser.getId().toString());
        verify(snapshotRepo, never()).save(any(Snapshot.class));

    }
    @Test
    public void CameraServiceTest_TakeSnapshot_ReturnsResultFailureWhenImageSaveFails(){
        Long userId = 1L;
        String username = "TestUser";
        String examId = Long.toString(ExamId);



        SnapshotResult failureResult = SnapshotResult.failure("Failed to save image");
        when(userRepo.findByUsername(username)).thenReturn(testUser);
        when(openCVWrapper.captureAndAnalyzeSnapshot(examId,userId.toString())).thenReturn(failureResult);

        String result= cameraService.takeSnapshot(username,ExamId);



        Assertions.assertThat(result).isEqualTo("Failed to save image");
        verify(userRepo).findByUsername(username);
        verify(openCVWrapper).captureAndAnalyzeSnapshot(examId,testUser.getId().toString());
        verify(snapshotRepo, never()).save(any(Snapshot.class));

    }
    @Test
    public void CameraServiceTest_TakeSnapshot_ReturnsInvalidUserException(){

        String username = "nonexistinguser";

        when(userRepo.findByUsername(username)).thenReturn(null);

        ApiRequestException exception = assertThrows(ApiRequestException.class, () -> cameraService.takeSnapshot(username,ExamId));

        Assertions.assertThat(exception.getMessage()).isEqualTo("User not found: "+username);
        verify(userRepo).findByUsername(username);
        verify(openCVWrapper,never()).captureAndAnalyzeSnapshot(any(),anyString());
        verify(snapshotRepo, never()).save(any(Snapshot.class));

    }

    @Test
    public void CameraServiceTest_TakeSnapshot_ReturnsOpenCVWrapperException(){

        Long userId = 1L;
        String username = "TestUser";
        String examId = Long.toString(ExamId);

        when(userRepo.findByUsername(username)).thenReturn(testUser);
        when(openCVWrapper.captureAndAnalyzeSnapshot(examId,userId.toString())).thenThrow(new RuntimeException("OpenCV Wrapper Exception"));

        String result= cameraService.takeSnapshot(username,ExamId);

        Assertions.assertThat(result).startsWith("Snapshot taking failed: ");
        Assertions.assertThat(result).contains("OpenCV Wrapper Exception");
        verify(userRepo).findByUsername(username);
        verify(openCVWrapper).captureAndAnalyzeSnapshot(examId,userId.toString());
        verify(snapshotRepo, never()).save(any(Snapshot.class));

    }

    @Test
    public void CameraServiceTest_TakeSnapshot_ReturnsInvalidSaveException(){

        Long userId = 1L;
        String username = "TestUser";
        String expectedFilePath = "/path/to/snapshot.jpg";
        String examId = Long.toString(ExamId);

        SnapshotResult successResult = SnapshotResult.success(expectedFilePath,true);

        when(userRepo.findByUsername(username)).thenReturn(testUser);
        when(openCVWrapper.captureAndAnalyzeSnapshot(examId,userId.toString())).thenReturn(successResult);
        when(snapshotRepo.save(any(Snapshot.class))).thenReturn(testSnapshot);

        String result= cameraService.takeSnapshot(username,ExamId);

        Assertions.assertThat(result).isEqualTo("Snapshot taken successfully");
        verify(snapshotRepo).save(argThat(snapshot->{
                assertAll(
                        ()-> Assertions.assertThat(snapshot.getUser()).isEqualTo(testUser),
                        ()-> Assertions.assertThat(snapshot.getStatus()).isEqualTo(true),
                        ()-> Assertions.assertThat(snapshot.getFilePath()).isEqualTo(expectedFilePath),
                        ()-> Assertions.assertThat(snapshot.isFaceIdentity()).isEqualTo(true),
                        ()-> Assertions.assertThat(snapshot.getCourseId()).isEqualTo(examId));
                return true;
                } )) ;


    }


}
