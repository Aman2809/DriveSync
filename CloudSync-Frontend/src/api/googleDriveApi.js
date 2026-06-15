// src/api/googleDriveApi.js
import { myAxios } from "./axiosConfig";

// ============= SYNC FOLDER OPERATIONS =============

// ✅ Check if sync folder exists
export const checkSyncFolder = async () => {
  try {
    const response = await myAxios.get("/api/drive/sync-folder");
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ✅ Create or ensure sync folder exists
export const ensureSyncFolder = async () => {
  try {
    const response = await myAxios.post("/api/drive/sync-folder");
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ============= FILE LISTING OPERATIONS =============

// ✅ Get all Google Drive files
export const listAllGoogleDriveFiles = async () => {
  try {
    const response = await myAxios.get("/api/drive/files");
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ✅ Get files in sync folder only
export const listSyncFolderFiles = async () => {
  try {
    const response = await myAxios.get("/api/drive/sync-folder/files");
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ============= FILE UPLOAD OPERATIONS =============

// ✅ Upload file to sync folder
export const uploadFileToSyncFolder = async (file) => {
  try {
    const formData = new FormData();
    formData.append("file", file);

    const response = await myAxios.post("/api/drive/upload", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });

    return response.data;
  } catch (error) {
    throw error;
  }
};

// ============= FILE METADATA OPERATIONS =============

// ✅ Get file metadata by ID
export const getFileMetadata = async (fileId) => {
  try {
    const response = await myAxios.get(`/api/drive/files/${fileId}`);
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ✅ Search file by name in sync folder
export const searchFileByName = async (fileName) => {
  try {
    const response = await myAxios.get("/api/drive/sync-folder/files/search", {
      params: { fileName }
    });
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ============= FILE DOWNLOAD OPERATIONS =============

// ✅ Download file by ID
export const downloadGoogleDriveFile = async (fileId) => {
  try {
    const response = await myAxios.get(`/api/drive/files/${fileId}/download`, {
      responseType: "blob", // Important for file download
    });
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ============= FILE UPDATE OPERATIONS =============

// ✅ Update existing file content
export const updateGoogleDriveFile = async (fileId, newFile) => {
  try {
    const formData = new FormData();
    formData.append("file", newFile);

    const response = await myAxios.put(`/api/drive/files/${fileId}`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });

    return response.data;
  } catch (error) {
    throw error;
  }
};

// ============= FILE DELETE OPERATIONS =============

// ✅ Delete file by ID
export const deleteGoogleDriveFile = async (fileId) => {
  try {
    const response = await myAxios.delete(`/api/drive/files/${fileId}`);
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ============= UTILITY OPERATIONS =============

// ✅ Test endpoint
export const testGoogleDriveConnection = async () => {
  try {
    const response = await myAxios.get("/api/drive/test");
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ✅ Get storage information
export const getStorageInfo = async () => {
  try {
    const response = await myAxios.get("/api/drive/storage-info");
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ✅ Check if file exists
export const checkFileExists = async (fileId) => {
  try {
    const response = await myAxios.get(`/api/drive/file-exists/${fileId}`);
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ✅ Get file content hash (MD5)
export const getFileContentHash = async (fileId) => {
  try {
    const response = await myAxios.get(`/api/drive/file-hash/${fileId}`);
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ✅ Health check for Google Drive service
export const googleDriveHealthCheck = async () => {
  try {
    const response = await myAxios.get("/api/drive/health");
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ============= HELPER FUNCTIONS =============

// ✅ Helper function to handle file download with proper filename
export const downloadFileWithFilename = async (fileId, customFilename = null) => {
  try {
    // First get file metadata to get the original filename
    const metadata = await getFileMetadata(fileId);
    const filename = customFilename || metadata.file.name;
    
    // Then download the file
    const fileBlob = await downloadGoogleDriveFile(fileId);
    
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
export const uploadFileWithProgress = async (file, onProgress = null) => {
  try {
    const formData = new FormData();
    formData.append("file", file);

    const response = await myAxios.post("/api/drive/upload", formData, {
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

// ============= BATCH OPERATIONS =============

// ✅ Upload multiple files
export const uploadMultipleFiles = async (files, onProgress = null) => {
  try {
    const uploadPromises = files.map((file, index) => 
      uploadFileWithProgress(file, (progress) => {
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
export const deleteMultipleFiles = async (fileIds) => {
  try {
    const deletePromises = fileIds.map(fileId => deleteGoogleDriveFile(fileId));
    const results = await Promise.allSettled(deletePromises);
    return results;
  } catch (error) {
    throw error;
  }
};