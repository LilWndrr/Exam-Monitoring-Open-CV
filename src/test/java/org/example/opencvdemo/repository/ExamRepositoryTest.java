package org.example.opencvdemo.repository;

import org.example.opencvdemo.entity.ExamIds;
import org.example.opencvdemo.entity.Snapshot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.repository.config.RepositoryConfiguration;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Optional;


@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class ExamRepositoryTest {

    @Autowired
    private ExamRepo examRepo;



    @Test
    public void ExamRepository_SaveAll_ReturnSavedExam(){
        //Arrange
        ExamIds examIds = ExamIds.builder().build();

        //Act
        ExamIds savedExamIds = examRepo.save(examIds);

        //Assert
        Assertions.assertNotNull(savedExamIds);
        Assertions.assertTrue(savedExamIds.getId() > 0);

    }

    @Test
    public void ExamRepository_GetAll_ReturnMoreThenOneExam(){
        //Arrange
        ExamIds examIds1 = ExamIds.builder().build();
        ExamIds examIds2 = ExamIds.builder().build();

        //Act
        ExamIds savedExamIds1 = examRepo.save(examIds1);
        ExamIds savedExamIds2 = examRepo.save(examIds2);


        List<ExamIds> savedExamIdsList = examRepo.findAll();
        //Assert
        Assertions.assertNotNull(savedExamIdsList);
        Assertions.assertTrue(savedExamIdsList.size()   > 0);
        Assertions.assertEquals(2, savedExamIdsList.size());

    }

    @Test
    public void ExamRepository_FindById_ReturnExactExam(){
        //Arrange
        ExamIds examIds1 = ExamIds.builder().build();


        //Act
        ExamIds savedExamIds1 = examRepo.save(examIds1);

        ExamIds examIds = examRepo.findById(savedExamIds1.getId()).get();


        //Assert
        Assertions.assertNotNull(examIds);


    }

    @Test
    public void ExamRepository_Delete_ReturnExamIsEmpty(){
        //Arrange
        ExamIds examIds1 = ExamIds.builder().build();


        //Act
        examRepo.save(examIds1);

        examRepo.delete(examIds1);

        Optional<ExamIds> examIds = examRepo.findById(examIds1.getId());


        //Assert
        Assertions.assertTrue(examIds.isEmpty());


    }
}
