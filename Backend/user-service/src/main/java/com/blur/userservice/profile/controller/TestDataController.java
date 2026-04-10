package com.blur.userservice.profile.controller;

import com.blur.userservice.profile.dto.ApiResponse;
import com.blur.userservice.profile.dto.request.UserCreationRequest;
import com.blur.userservice.profile.repository.UserProfileRepository;
import com.blur.userservice.profile.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/test-data")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class TestDataController {

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
    private static final String[] CITIES = {
            "Hà Nội", "Hồ Chí Minh", "Đà Nẵng", "Hải Phòng", "Cần Thơ",
            "Biên Hòa", "Nha Trang", "Huế", "Buôn Ma Thuột", "Quy Nhơn",
            "Vũng Tàu", "Đà Lạt", "Long Xuyên", "Thái Nguyên", "Nam Định",
            "Vinh", "Thanh Hóa", "Bắc Ninh"
    };

    UserService userService;
    UserProfileRepository userProfileRepository;

    /**
     * Endpoint gộp: Tạo users với city + random follows trong 1 lần gọi
     *
     * POST /test-data/generate-all?userCount=100&minFollows=5&maxFollows=50
     */
    @PostMapping("/generate-all")
    public ApiResponse<Map<String, Object>> generateAll(
            @RequestParam(defaultValue = "100") int userCount,
            @RequestParam(defaultValue = "5") int minFollows,
            @RequestParam(defaultValue = "50") int maxFollows) {

        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new LinkedHashMap<>();
        Random random = new Random();

        // ========== BƯỚC 1: Tạo users với city ==========
        AtomicInteger userSuccess = new AtomicInteger(0);
        AtomicInteger userFail = new AtomicInteger(0);
        Set<String> usedUsernames = new HashSet<>();

        for (int i = 0; i < userCount; i++) {
            try {
                String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
                String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
                String baseUsername = (firstName + lastName).toLowerCase();
                String username = generateUniqueUsername(baseUsername, usedUsernames);
                usedUsernames.add(username);

                UserCreationRequest request = UserCreationRequest.builder()
                        .username(username)
                        .email(username + "@testmail.com")
                        .password("Test@123456")
                        .firstName(firstName)
                        .lastName(lastName)
                        .dob(LocalDate.now().minusYears(20 + random.nextInt(30)))
                        .city(CITIES[random.nextInt(CITIES.length)])
                        .build();

                userService.createUser(request);
                userSuccess.incrementAndGet();
            } catch (Exception e) {
                userFail.incrementAndGet();
            }
        }

        long userDuration = System.currentTimeMillis() - startTime;
        result.put("usersCreated", userSuccess.get());
        result.put("usersFailed", userFail.get());
        result.put("usersDurationFormatted", formatDuration(userDuration));

        // ========== BƯỚC 2: Random follows ==========
        long followStart = System.currentTimeMillis();
        List<String> allProfileIds = userProfileRepository.findAllProfileIds();
        int profileCount = allProfileIds.size();
        AtomicInteger totalFollows = new AtomicInteger(0);
        AtomicInteger failedFollows = new AtomicInteger(0);

        if (profileCount >= 2) {
            int effectiveMax = Math.min(maxFollows, profileCount - 1);
            int effectiveMin = Math.min(minFollows, effectiveMax);

            for (int i = 0; i < profileCount; i++) {
                String fromId = allProfileIds.get(i);
                int followCount = random.nextInt(effectiveMax - effectiveMin + 1) + effectiveMin;

                Set<Integer> followedIndices = new HashSet<>();
                while (followedIndices.size() < followCount) {
                    int targetIndex = random.nextInt(profileCount);
                    if (targetIndex != i) {
                        followedIndices.add(targetIndex);
                    }
                }

                for (int targetIndex : followedIndices) {
                    try {
                        userProfileRepository.follow(fromId, allProfileIds.get(targetIndex));
                        totalFollows.incrementAndGet();
                    } catch (Exception e) {
                        failedFollows.incrementAndGet();
                    }
                }
            }

            // Cập nhật follow counts
            userProfileRepository.updateAllFollowCounts();
        }

        long followDuration = System.currentTimeMillis() - followStart;
        long totalDuration = System.currentTimeMillis() - startTime;

        result.put("totalProfiles", profileCount);
        result.put("followsCreated", totalFollows.get());
        result.put("followsFailed", failedFollows.get());
        result.put("avgFollowsPerUser", profileCount > 0 ? (double) totalFollows.get() / profileCount : 0);
        result.put("followsDurationFormatted", formatDuration(followDuration));
        result.put("totalDurationFormatted", formatDuration(totalDuration));

        return ApiResponse.<Map<String, Object>>builder()
                .code(1000)
                .message("Generate all completed: " + userSuccess.get() + " users + " + totalFollows.get() + " follows")
                .result(result)
                .build();
    }

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
                    String username = generateUniqueUsername(baseUsername, usedUsernames);
                    usedUsernames.add(username);

                    UserCreationRequest request = UserCreationRequest.builder()
                            .username(username)
                            .email(username + "@testmail.com")
                            .password("Test@123456")
                            .firstName(firstName)
                            .lastName(lastName)
                            .dob(LocalDate.now().minusYears(20 + random.nextInt(30)))
                            .build();

                    userService.createUser(request);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            }

            long duration = System.currentTimeMillis() - startTime;

            result.put("usersCreated", successCount.get());
            result.put("usersFailed", failCount.get());
            result.put("durationMs", duration);
            result.put("durationFormatted", formatDuration(duration));

            return ApiResponse.<Map<String, Object>>builder()
                    .code(1000)
                    .message("Users generated!")
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
        result.put("totalUsers", userProfileRepository.count());
        return ApiResponse.<Map<String, Object>>builder()
                .code(1000)
                .message("Stats retrieved")
                .result(result)
                .build();
    }

    private String generateUniqueUsername(String base, Set<String> used) {
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
