// ⚠️ Không còn cần các hàm này cho token management
// Giữ lại file này cho các mục đích khác (như lưu preferences)

// Các hàm setToken, getToken, removeToken không còn cần thiết
// vì token được quản lý bởi HttpOnly Cookie

// Giữ lại cho backward compatibility hoặc xóa hoàn toàn
export const setToken = (_token: string): void => {
    console.warn('setToken is deprecated. Token is now managed by HttpOnly Cookie.')
}

export const getToken = (): string | null => {
    console.warn('getToken is deprecated. Token is now managed by HttpOnly Cookie.')
    return null
}

export const removeToken = (): void => {
    console.warn('removeToken is deprecated. Token is now managed by HttpOnly Cookie.')
}
