package org.example.opencvdemo.repository;

import org.example.opencvdemo.entity.ExamIds;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRepo extends JpaRepository<ExamIds, Long> {

}
