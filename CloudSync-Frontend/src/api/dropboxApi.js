// src/api/dropboxApi.js
import { myAxios } from "./axiosConfig";

// ============= SYNC FOLDER OPERATIONS =============

// ✅ Check if sync folder exists
export const checkDropboxSyncFolder = async () => {
  try {
    const response = await myAxios.get("/api/dropbox/sync-folder");
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ✅ Create or ensure sync folder exists
export const ensureDropboxSyncFolder = async () => {
  try {
    const response = await myAxios.post("/api/dropbox/sync-folder");
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ============= FILE LISTING OPERATIONS =============

// ✅ Get all Dropbox files
export const listAllDropboxFiles = async () => {
  try {
    const response = await myAxios.get("/api/dropbox/files");
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ✅ Get files in sync folder only
export const listDropboxSyncFolderFiles = async () => {
  try {
    const response = await myAxios.get("/api/dropbox/sync-folder/files");
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ============= FILE UPLOAD OPERATIONS =============

// ✅ Upload file to sync folder
export const uploadFileToDropboxSyncFolder = async (file) => {
  try {
    const formData = new FormData();
    formData.append("file", file);

    const response = await myAxios.post("/api/dropbox/upload", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });

    return response.data;
  } catch (error) {
    throw error;
  }
};

// ============= FILE METADATA OPERATIONS =============

// ✅ Get file metadata by path
export const getDropboxFileMetadata = async (filePath) => {
  try {
    const response = await myAxios.get("/api/dropbox/files/metadata", {
      params: { filePath }
    });
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ✅ Search file by name in sync folder
export const searchDropboxFileByName = async (fileName) => {
  try {
    const response = await myAxios.get("/api/dropbox/sync-folder/files/search", {
      params: { fileName }
    });
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ============= FILE DOWNLOAD OPERATIONS =============

// ✅ Download file by path
export const downloadDropboxFile = async (filePath) => {
  try {
    const response = await myAxios.get("/api/dropbox/files/download", {
      params: { filePath },
      responseType: "blob", // Important for file download
    });
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ============= FILE UPDATE OPERATIONS =============

// ✅ Update existing file content
export const updateDropboxFile = async (filePath, newFile) => {
  try {
    const formData = new FormData();
    formData.append("file", newFile);

    const response = await myAxios.put("/api/dropbox/files/update", formData, {
      params: { filePath },
      headers: { "Content-Type": "multipart/form-data" },
    });

    return response.data;
  } catch (error) {
    throw error;
  }
};

// ============= FILE DELETE OPERATIONS =============

// ✅ Delete file by path
export const deleteDropboxFile = async (filePath) => {
  try {
    const response = await myAxios.delete("/api/dropbox/files/delete", {
      params: { filePath }
    });
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ============= STORAGE INFO OPERATIONS =============

// ✅ Get storage information
export const getDropboxStorageInfo = async () => {
  try {
    const response = await myAxios.get("/api/dropbox/storage-info");
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ============= UTILITY OPERATIONS =============

// ✅ Test endpoint
export const testDropboxConnection = async () => {
  try {
    const response = await myAxios.get("/api/dropbox/test");
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ✅ Check if file exists
export const checkDropboxFileExists = async (filePath) => {
  try {
    const response = await myAxios.get("/api/dropbox/file-exists", {
      params: { filePath }
    });
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ✅ Get file content hash
export const getDropboxFileContentHash = async (filePath) => {
  try {
    const response = await myAxios.get("/api/dropbox/file-hash", {
      params: { filePath }
    });
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ✅ Health check for Dropbox service
export const dropboxHealthCheck = async () => {
  try {
    const response = await myAxios.get("/api/dropbox/health");
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ============= HELPER FUNCTIONS =============

// ✅ Helper function to handle file download with proper filename
export const downloadDropboxFileWithFilename = async (filePath, customFilename = null) => {
  try {
    // First get file metadata to get the original filename
    const metadata = await getDropboxFileMetadata(filePath);
    const filename = customFilename || metadata.file.name;
    
    // Then download the file
    const fileBlob = await downloadDropboxFile(filePath);
    
    // Create download link
    const url = window.URL.createObjectURL(fileBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
    
    return { success: true, filename };
  } catch (error) {
    throw error;
  }
};

// ✅ Helper function to upload file with progress tracking
export const uploadDropboxFileWithProgress = async (file, onProgress = null) => {
  try {
    const formData = new FormData();
    formData.append("file", file);

    const response = await myAxios.post("/api/dropbox/upload", formData, {
      headers: { "Content-Type": "multipart/form-data" },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          onProgress(progress);
        }
      },
    });

    return response.data;
  } catch (error) {
    throw error;
  }
};

// ✅ Helper function to get file size in human readable format
export const getReadableFileSize = (bytes) => {
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  if (bytes === 0) return '0 Byte';
  const i = parseInt(Math.floor(Math.log(bytes) / Math.log(1024)));
  return Math.round(bytes / Math.pow(1024, i) * 100) / 100 + ' ' + sizes[i];
};

// ✅ Helper function to extract filename from path
export const extractFilenameFromPath = (filePath) => {
  return filePath.split('/').pop() || filePath;
};

// ✅ Helper function to extract directory from path
export const extractDirectoryFromPath = (filePath) => {
  const parts = filePath.split('/');
  parts.pop(); // Remove filename
  return parts.join('/') || '/';
};

// ============= BATCH OPERATIONS =============

// ✅ Upload multiple files
export const uploadMultipleDropboxFiles = async (files, onProgress = null) => {
  try {
    const uploadPromises = files.map((file, index) => 
      uploadDropboxFileWithProgress(file, (progress) => {
        if (onProgress) {
          onProgress(index, progress, file.name);
        }
      })
    );
    
    const results = await Promise.allSettled(uploadPromises);
    return results;
  } catch (error) {
    throw error;
  }
};

// ✅ Delete multiple files
export const deleteMultipleDropboxFiles = async (filePaths) => {
  try {
    const deletePromises = filePaths.map(filePath => deleteDropboxFile(filePath));
    const results = await Promise.allSettled(deletePromises);
    return results;
  } catch (error) {
    throw error;
  }
};

// ✅ Check multiple files existence
export const checkMultipleDropboxFilesExist = async (filePaths) => {
  try {
    const checkPromises = filePaths.map(filePath => 
      checkDropboxFileExists(filePath).then(result => ({
        filePath,
        exists: result.exists,
        success: result.success
      }))
    );
    
    const results = await Promise.allSettled(checkPromises);
    return results;
  } catch (error) {
    throw error;
  }
};

// ============= FILE PATH UTILITIES =============

// ✅ Normalize Dropbox file path
export const normalizeDropboxPath = (path) => {
  // Ensure path starts with /
  if (!path.startsWith('/')) {
    path = '/' + path;
  }
  
  // Remove double slashes
  path = path.replace(/\/+/g, '/');
  
  // Remove trailing slash unless it's root
  if (path !== '/' && path.endsWith('/')) {
    path = path.slice(0, -1);
  }
  
  return path;
};

// ✅ Join path components
export const joinDropboxPath = (...components) => {
  const path = components
    .filter(component => component && component.length > 0)
    .join('/')
    .replace(/\/+/g, '/');
  
  return normalizeDropboxPath(path);
};

// ============= SYNC FOLDER SPECIFIC OPERATIONS =============

// ✅ Get sync folder path (usually /CloudSync)
export const getSyncFolderPath = () => {
  return '/CloudSync'; // This should match your backend sync folder name
};

// ✅ Build file path in sync folder
export const buildSyncFolderFilePath = (filename) => {
  return joinDropboxPath(getSyncFolderPath(), filename);
};

// ✅ Upload file to sync folder with specific path
export const uploadFileToSyncFolderWithPath = async (file, filename = null) => {
  try {
    const targetFilename = filename || file.name;
    // The backend handles the sync folder path, so we just upload the file
    return await uploadFileToDropboxSyncFolder(file);
  } catch (error) {
    throw error;
  }
};

// ============= ERROR HANDLING HELPERS =============

// ✅ Check if error is authentication related
export const isDropboxAuthError = (error) => {
  return error.response?.status === 401 || 
         error.response?.data?.error?.includes('Not authenticated');
};

// ✅ Check if error is file not found
export const isDropboxFileNotFoundError = (error) => {
  return error.response?.status === 404 ||
         error.response?.data?.error?.includes('not found');
};

// ✅ Check if error is storage quota exceeded
export const isDropboxStorageFullError = (error) => {
  return error.response?.status === 507 ||
         error.response?.data?.error?.includes('storage') ||
         error.response?.data?.error?.includes('quota');
};