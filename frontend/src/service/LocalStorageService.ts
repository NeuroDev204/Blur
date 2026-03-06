// ⚠️ Không còn cần các hàm này cho token management
// Giữ lại file này cho các mục đích khác (như lưu preferences)

// Các hàm setToken, getToken, removeToken không còn cần thiết
// vì token được quản lý bởi HttpOnly Cookie

// Giữ lại cho backward compatibility hoặc xóa hoàn toàn
export const setToken = (_token: string): void => {
}

export const getToken = (): string | null => {
    return null
}

export const removeToken = (): void => {
}
