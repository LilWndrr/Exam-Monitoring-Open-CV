package org.example.opencvdemo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.security.Timestamp;
import java.util.Date;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Snapshot implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String UUID;

    @ManyToOne
    @JoinColumn(name = "user_id",referencedColumnName = "user_id")
    private User user ;

    private String courseId;

    private boolean faceIdentity;

    private Boolean status;

    private String filePath;








}
