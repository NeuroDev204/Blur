import axios, { AxiosInstance } from "axios"
import { CONFIG } from "./configuration"

const httpClient: AxiosInstance = axios.create({
  baseURL: CONFIG.API_GATEWAY,
  withCredentials: true, // gui cookies voi moi request
  headers: {
    "Content-Type": "application/json",
  }
});

httpClient.interceptors.request.use(
  (config) => {
    // cookie se duoc browser tu dong gui
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
)

// handle 401
httpClient.interceptors.response.use(
  (response) => response, async (error) => {
    if (error.response?.status === 401) {
      try {
        await axios.post("/api/auth/refresh", {}, { withCredentials: true });
        // retry original request
        return httpClient(error.config);

      } catch (refreshError) {
        // Xóa flag authenticated
        localStorage.removeItem("token");
        //redirect to login
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  }
);


export default httpClient;
