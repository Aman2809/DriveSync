import { myAxios, BASE_URL } from "./axiosConfig";

// Fetch connected drives for a user
export const fetchConnectedDrives = async (userId) => {
  try {
    const response = await myAxios.get(`/api/users/${userId}/providers`);
    // Backend returns: { providers: ["GOOGLE", "DROPBOX"] }
    return response.data.providers.map((provider) => ({
      id: provider.toLowerCase(),
      name: provider === "GOOGLE" ? "Google Drive" : 
            provider === "DROPBOX" ? "Dropbox" : 
            provider === "ONEDRIVE" ? "OneDrive" : provider
    }));
  } catch (error) {
    console.error("Failed to fetch drives:", error);
    return [];
  }
};

// Redirect to backend OAuth2 login flow
export const connectGoogleDrive = () => {
  window.location.href = `${BASE_URL}/oauth2/authorization/google`;
};

export const connectDropbox = () => {
  window.location.href = `${BASE_URL}/oauth2/authorization/dropbox`;
};

// If you add OneDrive in backend later
export const connectOneDrive = () => {
  window.location.href = `${BASE_URL}/oauth2/authorization/onedrive`;
};
