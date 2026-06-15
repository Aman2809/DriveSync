// src/api/syncApi.js
import { myAxios } from "./axiosConfig";

// ============= ONE-WAY SYNC OPERATIONS =============

// ✅ Sync from Dropbox to Google Drive (one-way)
export const syncDropboxToGoogleDrive = async (dropboxToken, gdriveToken) => {
  try {
    const response = await myAxios.post("/api/sync/oneway/dropbox-to-gdrive", null, {
      params: {
        dropboxToken,
        gdriveToken
      }
    });
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ✅ Sync from Google Drive to Dropbox (one-way)
export const syncGoogleDriveToDropbox = async (gdriveToken, dropboxToken) => {
  try {
    const response = await myAxios.post("/api/sync/oneway/gdrive-to-dropbox", null, {
      params: {
        gdriveToken,
        dropboxToken
      }
    });
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ============= BIDIRECTIONAL SYNC OPERATIONS =============

// ✅ Bidirectional sync between Google Drive and Dropbox
export const syncBidirectional = async (gdriveToken, dropboxToken) => {
  try {
    const response = await myAxios.post("/api/sync/bidirectional", null, {
      params: {
        gdriveToken,
        dropboxToken
      }
    });
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ============= HELPER FUNCTIONS FOR TOKEN MANAGEMENT =============

// ✅ Helper to get tokens from OAuth clients (if available in your app context)
export const getTokensFromStorage = () => {
  try {
    // This would depend on how you store OAuth tokens in your app
    // You might get them from localStorage, sessionStorage, or Redux store
    const gdriveToken = localStorage.getItem('gdrive_token');
    const dropboxToken = localStorage.getItem('dropbox_token');
    
    return { gdriveToken, dropboxToken };
  } catch (error) {
    console.error('Error getting tokens from storage:', error);
    return { gdriveToken: null, dropboxToken: null };
  }
};

// ✅ Helper to validate tokens exist
export const validateTokens = (gdriveToken, dropboxToken) => {
  if (!gdriveToken || !dropboxToken) {
    throw new Error('Both Google Drive and Dropbox tokens are required for sync operations');
  }
  return true;
};

// ============= SYNC WITH AUTO TOKEN RETRIEVAL =============

// ✅ Sync Dropbox to Google Drive with auto token retrieval
export const syncDropboxToGoogleDriveAuto = async () => {
  try {
    const { gdriveToken, dropboxToken } = getTokensFromStorage();
    validateTokens(gdriveToken, dropboxToken);
    
    return await syncDropboxToGoogleDrive(dropboxToken, gdriveToken);
  } catch (error) {
    throw error;
  }
};

// ✅ Sync Google Drive to Dropbox with auto token retrieval
export const syncGoogleDriveToDropboxAuto = async () => {
  try {
    const { gdriveToken, dropboxToken } = getTokensFromStorage();
    validateTokens(gdriveToken, dropboxToken);
    
    return await syncGoogleDriveToDropbox(gdriveToken, dropboxToken);
  } catch (error) {
    throw error;
  }
};

// ✅ Bidirectional sync with auto token retrieval
export const syncBidirectionalAuto = async () => {
  try {
    const { gdriveToken, dropboxToken } = getTokensFromStorage();
    validateTokens(gdriveToken, dropboxToken);
    
    return await syncBidirectional(gdriveToken, dropboxToken);
  } catch (error) {
    throw error;
  }
};

// ============= SYNC WITH PROGRESS TRACKING =============

// ✅ Sync with progress callback (simulated progress since backend doesn't provide it)
export const syncDropboxToGoogleDriveWithProgress = async (
  dropboxToken, 
  gdriveToken, 
  onProgress = null
) => {
  try {
    if (onProgress) onProgress(0, 'Starting sync from Dropbox to Google Drive...');
    
    const result = await syncDropboxToGoogleDrive(dropboxToken, gdriveToken);
    
    if (onProgress) onProgress(100, 'Sync from Dropbox to Google Drive completed!');
    
    return result;
  } catch (error) {
    if (onProgress) onProgress(-1, `Sync failed: ${error.message}`);
    throw error;
  }
};

// ✅ Sync with progress callback (Google Drive to Dropbox)
export const syncGoogleDriveToDropboxWithProgress = async (
  gdriveToken, 
  dropboxToken, 
  onProgress = null
) => {
  try {
    if (onProgress) onProgress(0, 'Starting sync from Google Drive to Dropbox...');
    
    const result = await syncGoogleDriveToDropbox(gdriveToken, dropboxToken);
    
    if (onProgress) onProgress(100, 'Sync from Google Drive to Dropbox completed!');
    
    return result;
  } catch (error) {
    if (onProgress) onProgress(-1, `Sync failed: ${error.message}`);
    throw error;
  }
};

// ✅ Bidirectional sync with progress callback
export const syncBidirectionalWithProgress = async (
  gdriveToken, 
  dropboxToken, 
  onProgress = null
) => {
  try {
    if (onProgress) onProgress(0, 'Starting bidirectional sync...');
    
    const result = await syncBidirectional(gdriveToken, dropboxToken);
    
    if (onProgress) onProgress(100, 'Bidirectional sync completed!');
    
    return result;
  } catch (error) {
    if (onProgress) onProgress(-1, `Sync failed: ${error.message}`);
    throw error;
  }
};

// ============= SYNC STATUS AND UTILITIES =============

// ✅ Check if sync is possible (tokens are available)
export const canSync = () => {
  try {
    const { gdriveToken, dropboxToken } = getTokensFromStorage();
    return !!(gdriveToken && dropboxToken);
  } catch (error) {
    console.error('Error checking sync capability:', error);
    return false;
  }
};

// ✅ Get sync requirements status
export const getSyncRequirements = () => {
  const { gdriveToken, dropboxToken } = getTokensFromStorage();
  
  return {
    hasGoogleDriveToken: !!gdriveToken,
    hasDropboxToken: !!dropboxToken,
    canSync: !!(gdriveToken && dropboxToken),
    missingTokens: [
      !gdriveToken ? 'Google Drive' : null,
      !dropboxToken ? 'Dropbox' : null
    ].filter(Boolean)
  };
};


// ✅ List files inside the sync folder
export const listSyncFolderFiles = async () => {
  try {
    const response = await myAxios.get("/sync/folder/files");
    return response.data;
  } catch (error) {
    throw error;
  }
};

// ✅ Check if sync folder exists
export const checkSyncFolder = async () => {
  try {
    const response = await myAxios.get("/sync/folder/check");
    return response.data;
  } catch (error) {
    throw error;
  }
};


// ============= ERROR HANDLING HELPERS =============

// ✅ Check if error is related to invalid tokens
export const isSyncTokenError = (error) => {
  return error.response?.status === 401 || 
         error.message?.includes('Invalid tokens') ||
         error.response?.data?.includes('Invalid tokens');
};

// ✅ Check if error is related to user not found
export const isSyncUserNotFoundError = (error) => {
  return error.response?.status === 401 && 
         (error.response?.data?.includes('user not found') ||
          error.message?.includes('user not found'));
};

// ✅ Check if error is related to sync service failure
export const isSyncServiceError = (error) => {
  return error.response?.status === 500 &&
         (error.response?.data?.includes('Error during sync') ||
          error.message?.includes('Error during sync'));
};

// ============= SYNC OPERATION TYPES =============

// ✅ Enum-like object for sync operations
export const SYNC_OPERATIONS = {
  DROPBOX_TO_GDRIVE: 'dropbox-to-gdrive',
  GDRIVE_TO_DROPBOX: 'gdrive-to-dropbox',
  BIDIRECTIONAL: 'bidirectional'
};

// ✅ Execute sync based on operation type
export const executeSyncOperation = async (operationType, gdriveToken, dropboxToken, onProgress = null) => {
  try {
    validateTokens(gdriveToken, dropboxToken);
    
    switch (operationType) {
      case SYNC_OPERATIONS.DROPBOX_TO_GDRIVE:
        return await syncDropboxToGoogleDriveWithProgress(dropboxToken, gdriveToken, onProgress);
      
      case SYNC_OPERATIONS.GDRIVE_TO_DROPBOX:
        return await syncGoogleDriveToDropboxWithProgress(gdriveToken, dropboxToken, onProgress);
      
      case SYNC_OPERATIONS.BIDIRECTIONAL:
        return await syncBidirectionalWithProgress(gdriveToken, dropboxToken, onProgress);
      
      default:
        throw new Error(`Unknown sync operation: ${operationType}`);
    }
  } catch (error) {
    throw error;
  }
};

// ============= BATCH SYNC OPERATIONS =============

// ✅ Execute multiple sync operations in sequence
export const executeBatchSync = async (operations, gdriveToken, dropboxToken, onProgress = null) => {
  try {
    validateTokens(gdriveToken, dropboxToken);
    
    const results = [];
    const totalOperations = operations.length;
    
    for (let i = 0; i < operations.length; i++) {
      const operation = operations[i];
      
      if (onProgress) {
        onProgress(
          Math.round((i / totalOperations) * 100), 
          `Executing operation ${i + 1}/${totalOperations}: ${operation}`
        );
      }
      
      try {
        const result = await executeSyncOperation(operation, gdriveToken, dropboxToken);
        results.push({ operation, success: true, result });
      } catch (error) {
        results.push({ operation, success: false, error: error.message });
      }
    }
    
    if (onProgress) {
      onProgress(100, 'All sync operations completed');
    }
    
    return results;
  } catch (error) {
    throw error;
  }
};