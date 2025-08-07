package org.example.opencvdemo.helper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
public class SnapshotResult {
    private final String filePath;
    private final boolean isValid;
    private final boolean faceIdentityValid;
    private final String errorMessage;

    public SnapshotResult(String filePath, boolean isValid, boolean faceIdentityValid) {
        this(filePath, isValid, faceIdentityValid, null);
    }

    public static SnapshotResult success(String filePath, boolean faceIdentityValid) {
        return new SnapshotResult(filePath, true, faceIdentityValid);
    }

    public static SnapshotResult failure(String errorMessage) {
        return new SnapshotResult(null, false, false, errorMessage);
    }
}
