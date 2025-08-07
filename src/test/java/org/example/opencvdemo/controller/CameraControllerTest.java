package org.example.opencvdemo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.opencvdemo.controllers.AuthenticationController;
import org.example.opencvdemo.controllers.CameraController;
import org.example.opencvdemo.entity.Snapshot;
import org.example.opencvdemo.entity.User;
import org.example.opencvdemo.service.CameraServiceTest;
import org.example.opencvdemo.services.CameraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static java.lang.Boolean.TRUE;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

@WebMvcTest(CameraController.class)
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
public class CameraControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CameraService cameraService;
    @MockitoBean
    private TaskScheduler taskScheduler;

    @MockitoBean
    private ScheduledFuture<?> mockScheduledFuture;
    @Autowired
    private ObjectMapper objectMapper;

    List<Snapshot> snapshots;
    long examId = 1L;

    @BeforeEach
    void setUp() {
        snapshots = new ArrayList<>();

        User testUser = User.builder().username("TestUser").id(1L).password("password").roles(new HashSet<>()).build();

        Snapshot testSnapshot = Snapshot.builder().UUID("uuid").user(testUser).courseId(Long.toString(examId)).status(true).faceIdentity(true).filePath("test/filepath/snapshot1.jpg").build();
        snapshots.add(testSnapshot);
        snapshots.add(Snapshot.builder().UUID("uuid2").user(testUser).courseId(Long.toString(examId)).status(true).faceIdentity(true).filePath("test/filepath/snapshot2.jpg").build());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    public void CameraControllerTest_getAll_ReturnSnapshots()throws Exception {
        when(cameraService.getAll()).thenReturn(snapshots);
        mockMvc.perform(get("/admin/getAll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf())).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(snapshots.size()));

        verify(cameraService).getAll();

    }
    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    public void CameraControllerTest_getByUserId_ReturnSnapshots()throws Exception {
        long userId = 1L;
        when(cameraService.getByUserId(userId)).thenReturn(snapshots);
        mockMvc.perform(get("/admin/getByUserId")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("userId", Long.toString(userId))
                        .with(csrf())).andExpect(status().isOk())
                        .andExpect(jsonPath("$.length()").value(snapshots.size()));

        verify(cameraService).getByUserId(userId);

    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    public void CameraControllerTest_getByUserIdAndExamId_ReturnSnapshots()throws Exception {
        long userId = 1L;

        when(cameraService.getByUserIdAndExamId(userId,Long.toString(examId))).thenReturn(snapshots);
        mockMvc.perform(get("/admin/getByUserIdAndExamId")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("userId", Long.toString(userId))
                        .param("examId", Long.toString(examId))
                        .with(csrf())).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(snapshots.size()));

        verify(cameraService).getByUserIdAndExamId(userId,Long.toString(examId));
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER",username = "TestUser")
    public void CameraControllerTest_startCapturing_ReturnStartedResult()throws Exception {


        mockMvc.perform(get("/user/startScheduler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf())).andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("Started"));

        verify(taskScheduler).scheduleAtFixedRate(any(Runnable.class),eq(1000L));

    }

    @Test
    @WithMockUser(authorities = "ROLE_USER",username = "TestUser")
    public void CameraControllerTest_startCapturing_ReturnIsAlreadyRunningResult()throws Exception {

        mockMvc.perform(get("/user/startScheduler").with(csrf()));

        mockMvc.perform(get("/user/startScheduler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf())).andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("Is already running"));

        verify(taskScheduler,never()).scheduleAtFixedRate(any(Runnable.class),any(Duration.class));

    }

    @Test
    @WithMockUser(authorities = "ROLE_USER", username = "TestUser")
    public void CameraControllerTest_startCapturing_CallsCameraService() throws Exception {
        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);

        mockMvc.perform(get("/user/startScheduler")
                .with(csrf()));

        verify(taskScheduler).scheduleAtFixedRate(taskCaptor.capture(), eq (1000L));

        Runnable capturedTask = taskCaptor.getValue();
        capturedTask.run();

        verify(cameraService).takeSnapshot(any(), any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER", username = "TestUser")
    public void CameraControllerTest_stopCapturing_IsNotRunning() throws Exception {


        mockMvc.perform(get("/user/stopCapturing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf())).andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("Is not running"));


    }
    @Test
    @WithMockUser(authorities = "ROLE_USER", username = "TestUser")
    public void CameraControllerTest_stopCapturing_StopsCapturing() throws Exception {
        when(taskScheduler.scheduleAtFixedRate(any(Runnable.class), anyLong()))
                .thenReturn((ScheduledFuture) mockScheduledFuture);
        mockMvc.perform(get("/user/startScheduler").with(csrf()));

        mockMvc.perform(get("/user/stopCapturing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf())).andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("Stopped"));

        verify(mockScheduledFuture).cancel(false);
    }

}
