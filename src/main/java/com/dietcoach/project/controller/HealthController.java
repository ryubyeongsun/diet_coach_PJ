package com.dietcoach.project.controller;

import com.dietcoach.project.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서버 헬스 체크용 컨트롤러
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<String>> health() {
        // message에 사람이 읽을 메시지, data에는 실제 상태 값
        ApiResponse<String> body =
                ApiResponse.success("dietcoach-backend is up 🚀", "UP");

        return ResponseEntity.ok(body);
    }
}
