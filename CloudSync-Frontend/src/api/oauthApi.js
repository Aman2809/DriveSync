// src/api/oauthApi.js
import { myAxios } from "./axiosConfig";

// ============= AUTH STATUS =============

// Get current logged-in user (Spring Security provides this via /user)
export const getCurrentUser = async () => {
  try {
    const response = await myAxios.get("/api/user"); // <-- backend endpoint
    return response.data;
  } catch (error) {
    return null;
  }
};

// Logout user
export const logoutUser = async () => {
  try {
    await myAxios.post("/logout"); // Spring Security default logout
    return true;
  } catch (error) {
    console.error("Logout failed:", error);
    return false;
  }
};

// Check if logged in
export const isLoggedIn = async () => {
  const user = await getCurrentUser();
  return user !== null;
};
