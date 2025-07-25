package org.example.opencvdemo.services;

import org.example.opencvdemo.entity.ExamIds;
import org.example.opencvdemo.repository.ExamRepo;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExamService {

    private final ExamRepo examRepo;


    public ExamService(ExamRepo examRepo) {
        this.examRepo = examRepo;
    }


    //@Cacheable("exams")
    public List<ExamIds> getALl(){
        return examRepo.findAll();
    }
}
