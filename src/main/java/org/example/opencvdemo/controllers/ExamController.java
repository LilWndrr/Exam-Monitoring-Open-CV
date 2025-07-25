package org.example.opencvdemo.controllers;


import org.example.opencvdemo.entity.ExamIds;
import org.example.opencvdemo.services.ExamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/exam")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }


    @GetMapping("getAll")

    public ResponseEntity<List<ExamIds>> getALl(){
        return ResponseEntity.ok(examService.getALl());
    }
}
