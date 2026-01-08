import axios, { AxiosInstance } from "axios"
import { CONFIG } from "./configuration"

const httpClient: AxiosInstance = axios.create({
    baseURL: CONFIG.API_GATEWAY,
    timeout: 10000,
    headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${localStorage.getItem("token") || ""}`,
    }
})

export default httpClient
