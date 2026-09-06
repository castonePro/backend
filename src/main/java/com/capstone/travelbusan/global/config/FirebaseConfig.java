package com.capstone.travelbusan.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

// firebase-adminsdk.json은 .gitignore 대상(민감 정보)이라 각자 로컬/서버에 개별 배치해야 한다.
// 파일이 없어도 서버 전체가 죽지 않고, 푸시 알림 기능만 비활성화된 채로 나머지 기능은 정상 동작하게 처리.
@Slf4j
@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        if (!FirebaseApp.getApps().isEmpty()) return;

        try {
            InputStream serviceAccount = getClass()
                    .getClassLoader()
                    .getResourceAsStream("firebase-adminsdk.json");

            // 클래스패스에 없으면 현재 작업 디렉토리(외부 마운트 파일)에서도 탐색
            if (serviceAccount == null) {
                java.io.File file = new java.io.File("firebase-adminsdk.json");
                if (file.exists()) {
                    serviceAccount = new java.io.FileInputStream(file);
                }
            }

            if (serviceAccount == null) {
                log.warn("firebase-adminsdk.json이 없어 Firebase(푸시 알림)를 초기화하지 않습니다. " +
                        "src/main/resources/firebase-adminsdk.json을 배치하면 활성화됩니다.");
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("🔥 [Firebase] Firebase Admin SDK가 성공적으로 초기화되었습니다.");
        } catch (IOException e) {
            log.warn("Firebase 초기화 실패 — 푸시 알림 없이 나머지 기능은 정상 동작합니다.", e);
        }
    }
}