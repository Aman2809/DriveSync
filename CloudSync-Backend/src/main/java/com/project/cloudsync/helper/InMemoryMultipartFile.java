package com.project.cloudsync.helper;

import org.springframework.mock.web.MockMultipartFile;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class InMemoryMultipartFile extends MockMultipartFile {

    public InMemoryMultipartFile(String fileName, byte[] content) throws IOException {
        super(fileName, fileName, "application/octet-stream", new ByteArrayInputStream(content));
    }
}

