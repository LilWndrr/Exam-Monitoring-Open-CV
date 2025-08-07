package org.example.opencvdemo.service;

import org.assertj.core.api.Assertions;
import org.example.opencvdemo.entity.ExamIds;
import org.example.opencvdemo.repository.ExamRepo;
import org.example.opencvdemo.services.ExamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Publisher;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class ExamServiceTests {
    @Mock
    private ExamRepo examRepo;

    @InjectMocks
    private ExamService examService;

    @Test
    public void examService_CreateExam_ReturnsSavedExam(){



        ExamIds examIds = ExamIds.builder().build();
        when( examRepo.save(Mockito.any(ExamIds.class))).thenReturn(examIds);

        ExamIds saveExam = examService.add();

        Assertions.assertThat(saveExam).isNotNull();

    }

    @Test
    public void examService_GetAll_ReturnsAllExams(){

        List<ExamIds> expectedExams = Arrays.asList(ExamIds.builder().build(),ExamIds.builder().build());


        when( examRepo.findAll()).thenReturn(expectedExams);

        List<ExamIds> result = examService.getALl();


        Assertions.assertThat(result).isNotNull()   ;
        Assertions.assertThat(result).containsExactlyElementsOf(expectedExams);
        Assertions.assertThat(result).hasSize(2);

    }
}
