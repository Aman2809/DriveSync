//package com.project.cloudsync.scheduler;
//
//import com.project.cloudsync.entities.User;
//import com.project.cloudsync.repositories.UserRepository;
//import com.project.cloudsync.service.TokenRefreshService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class TokenRefreshScheduler {
//
//    private final UserRepository userRepository;
//    private final TokenRefreshService tokenRefreshService;
//
//    // Runs every 30 minutes
//    @Scheduled(fixedRate = 30 * 60 * 1000)
//    public void refreshTokens() {
//        log.info("🔄 Running scheduled token refresh job...");
//
//        for (User user : userRepository.findAll()) {
//            if (user.getGoogleRefreshToken() != null) {
//                tokenRefreshService.refreshGoogleToken(user);
//            }
//            if (user.getDropboxRefreshToken() != null) {
//                tokenRefreshService.refreshDropboxToken(user);
//            }
//        }
//    }
//}
