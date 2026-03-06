package com.blur.userservice.identity.controller;

import com.blur.userservice.identity.dto.request.ApiResponse;
import com.blur.userservice.identity.dto.request.UserCreationRequest;
import com.blur.userservice.identity.repository.UserRepository;
import com.blur.userservice.identity.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/test-data")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class TestDataController {

    // Danh sách họ và tên để tạo user ngẫu nhiên
    private static final String[] FIRST_NAMES = {
            "An", "Binh", "Cuong", "Dung", "Duc", "Giang", "Hai", "Hung", "Khang", "Long",
            "Minh", "Nam", "Phong", "Quang", "Son", "Tam", "Thang", "Tuan", "Viet", "Vu",
            "Anh", "Chi", "Dung", "Hanh", "Huong", "Lan", "Linh", "Mai", "Ngoc", "Phuong",
            "Thao", "Trang", "Trinh", "Uyen", "Yen", "Bao", "Dat", "Hoang", "Khanh", "Thinh"
    };
    private static final String[] LAST_NAMES = {
            "Nguyen", "Tran", "Le", "Pham", "Hoang", "Huynh", "Phan", "Vu", "Vo", "Dang",
            "Bui", "Do", "Ho", "Ngo", "Duong", "Ly", "Truong", "Dinh", "Doan", "Luong"
    };
    UserService userService;
    UserRepository userRepository;

    @PostMapping("/generate")
    public ApiResponse<Map<String, Object>> generateUsers(@RequestParam(defaultValue = "10000") int userCount) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);


        try {
            Random random = new Random();
            Set<String> usedUsernames = new HashSet<>();

            for (int i = 0; i < userCount; i++) {
                try {
                    String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
                    String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
                    String baseUsername = (firstName + lastName).toLowerCase();
                    String username = generateUniqueUsername(baseUsername, usedUsernames, random);
                    usedUsernames.add(username);

                    // Tạo request đăng ký user
                    UserCreationRequest request = UserCreationRequest.builder()
                            .username(username)
                            .email(username + "@testmail.com")
                            .password("Test@123456")
                            .firstName(firstName)
                            .lastName(lastName)
                            .dob(LocalDate.now().minusYears(20 + random.nextInt(30)))
                            .build();

                    // Gọi service register
                    userService.createUser(request);
                    successCount.incrementAndGet();

                    // Log tiến độ mỗi 500 users
                    if ((i + 1) % 500 == 0) {
                    }

                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            }

            long duration = System.currentTimeMillis() - startTime;

            result.put("usersCreated", successCount.get());
            result.put("usersFailed", failCount.get());
            result.put("durationMs", duration);
            result.put("durationFormatted", formatDuration(duration));
            result.put("nextStep", "Now call POST /profile/internal/generate-follows to create random follows");


            return ApiResponse.<Map<String, Object>>builder()
                    .code(1000)
                    .message("Users generated! Now call profile-service to create follows.")
                    .result(result)
                    .build();

        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("usersCreatedBeforeError", successCount.get());
            return ApiResponse.<Map<String, Object>>builder()
                    .code(5000)
                    .message("Error: " + e.getMessage())
                    .result(result)
                    .build();
        }
    }


    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalUsers", userRepository.count());
        return ApiResponse.<Map<String, Object>>builder()
                .code(1000)
                .message("Stats retrieved")
                .result(result)
                .build();
    }

    private String generateUniqueUsername(String base, Set<String> used, Random random) {
        String username = base;
        int counter = 0;
        while (used.contains(username)) {
            username = base + (++counter);
        }
        return username;
    }

    private String formatDuration(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes > 0) {
            return String.format("%d min %d sec", minutes, seconds);
        }
        return String.format("%d sec %d ms", seconds, ms % 1000);
    }
}
