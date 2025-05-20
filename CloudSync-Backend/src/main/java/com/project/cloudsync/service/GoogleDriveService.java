//package com.project.cloudsync.service;
//
//import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
//import com.google.api.client.json.jackson2.JacksonFactory;
//import com.google.api.services.drive.Drive;
//import com.google.api.services.drive.model.FileList;
//import org.springframework.stereotype.Service;
//
//import java.io.IOException;
//import java.security.GeneralSecurityException;
//import java.util.Collections;
//
//@Service
//public class GoogleDriveService {
//
//    public Drive getDriveService(String accessToken) throws GeneralSecurityException, IOException {
//        var credential = new com.google.auth.oauth2.AccessToken(accessToken, null);
//        var credentials = com.google.auth.oauth2.GoogleCredentials.create(credential)
//                .createScoped(Collections.singleton("https://www.googleapis.com/auth/drive"));
//
//        var transport = GoogleNetHttpTransport.newTrustedTransport();
//        var jsonFactory = JacksonFactory.getDefaultInstance();
//
//        return new Drive.Builder(transport, jsonFactory, new com.google.api.client.http.HttpCredentialsAdapter(credentials))
//                .setApplicationName("CloudSync")
//                .build();
//    }
//
//    public void listFiles(String accessToken) throws Exception {
//        Drive driveService = getDriveService(accessToken);
//        FileList result = driveService.files().list()
//                .setPageSize(10)
//                .setFields("files(id, name)")
//                .execute();
//
//        System.out.println("Files:");
//        for (com.google.api.services.drive.model.File file : result.getFiles()) {
//            System.out.printf("%s (%s)\n", file.getName(), file.getId());
//        }
//    }
//}
