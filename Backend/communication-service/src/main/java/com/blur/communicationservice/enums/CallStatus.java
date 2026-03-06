package com.blur.communicationservice.enums;

public enum CallStatus {
    INITIATING, // Bắt đầu cuộc gọi
    RINGING, // Đang đổ chuông
    ANSWERED, // Đã nhấc máy
    REJECTED, // Từ chối
    MISSED, // Nhỡ cuộc gọi
    ENDED, // Kết thúc bình thường
    BUSY, // Đang bận
    FAILED // Lỗi kết nối
}
