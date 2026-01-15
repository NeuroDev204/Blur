package com.blur.chatservice.controller;

import com.blur.chatservice.entity.CallSession;
import com.blur.chatservice.service.CallService;
import com.blur.common.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/phones")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CallController {

  CallService callService;

  @GetMapping("/history")
  public ApiResponse<Page<CallSession>> getCallHistory(
      @RequestParam String userId,
      @RequestParam(defaultValue = "0") Integer page,
      @RequestParam(defaultValue = "20") Integer size) {
    Page<CallSession> callHistory = callService.getCallHistory(userId, page, size);
    return ApiResponse.<Page<CallSession>>builder().result(callHistory).build();
  }

  @GetMapping("/missed/count")
  public ApiResponse<Map<String, Long>> getMissedCount(@RequestParam String userId) {
    long count = callService.countMissedCalls(userId);
    return ApiResponse.<Map<String, Long>>builder()
        .result(Map.of("count", count))
        .build();
  }

  @GetMapping("/current")
  public ApiResponse<Optional<CallSession>> getCurrentCall(@RequestParam String userId) {
    return ApiResponse.<Optional<CallSession>>builder()
        .result(callService.getUserCurrentCall(userId))
        .build();
  }

  @PostMapping("/missed/mard-read")
  public ApiResponse<Void> markMissedCallAsRead(@RequestParam String userId) {
    callService.markMissedCallsAsRead(userId);
    return ApiResponse.<Void>builder().code(200).build();
  }
}
