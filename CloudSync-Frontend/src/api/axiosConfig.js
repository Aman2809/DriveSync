// src/api/axiosConfig.js
import axios from "axios";

// ❌ Remove /api/v1 here because your Spring Security endpoints like /login, /logout, /user
// are NOT under /api/v1 — only your drive APIs are.
// ✅ Keep base URL as backend root

export const BASE_URL = "http://localhost:8080";

export const myAxios = axios.create({
  baseURL: BASE_URL,
  headers: { "Content-Type": "application/json" },
  timeout: 30000,
  withCredentials: true,
});

// Optional: interceptors...


// Request interceptor (optional, good for debugging)
myAxios.interceptors.request.use(
  (config) => {
    console.log(`🚀 ${config.method?.toUpperCase()} ${config.url}`);
    return config;
  },
  (error) => {
    console.error("Request error:", error);
    return Promise.reject(error);
  }
);

// Response interceptor
myAxios.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error("API Error:", error.response?.data || error.message);

    if (error.response?.status === 401) {
      console.warn("⚠️ Not authenticated - redirect to login page");
      // 👉 You can add redirect logic here (e.g. window.location.href = "/login")
    } else if (error.response?.status === 403) {
      console.warn("⚠️ Forbidden - insufficient permissions");
    } else if (error.response?.status >= 500) {
      console.error("⚠️ Server error occurred");
    }

    return Promise.reject(error);
  }
);
