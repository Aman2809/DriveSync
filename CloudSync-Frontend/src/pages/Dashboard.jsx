import { useState, useEffect } from "react";
import { HardDrive, CheckCircle, Loader2, AlertCircle } from "lucide-react";
import { getCurrentUser } from "../api/oauthApi";
import { fetchConnectedDrives, connectGoogleDrive, connectDropbox, connectOneDrive } from "../api/driveApi";
import { syncBidirectionalAuto, canSync } from "../api/syncApi";

export default function Dashboard() {
  const [user, setUser] = useState(null);
  const [connectedDrives, setConnectedDrives] = useState([]);
  const [loading, setLoading] = useState(false);
  const [syncLoading, setSyncLoading] = useState(false);
  const [error, setError] = useState(null);
  const [syncSuccess, setSyncSuccess] = useState(false);

  useEffect(() => {
    init();
  }, []);

  const init = async () => {
    try {
      const currentUser = await getCurrentUser();
      if (!currentUser) {
        setError("Not logged in");
        return;
      }
      setUser(currentUser);
      await loadDrives(currentUser.id);
    } catch (err) {
      console.error(err);
      setError("Failed to load user info");
    }
  };

  const loadDrives = async (userId) => {
    try {
      const drives = await fetchConnectedDrives(userId);
      setConnectedDrives(drives);
    } catch (err) {
      console.error(err);
      setError("Failed to load connected drives");
    }
  };

  const handleConnectDrive = async (driveType) => {
    setLoading(true);
    if (driveType === "google") connectGoogleDrive();
    if (driveType === "dropbox") connectDropbox();
    if (driveType === "onedrive") connectOneDrive();
    setLoading(false);
  };

  const handleSyncNow = async () => {
    if (!canSync(connectedDrives)) {
      setError("Please connect at least two drives to sync");
      return;
    }
    try {
      setSyncLoading(true);
      await syncBidirectionalAuto();
      setSyncSuccess(true);
      setTimeout(() => setSyncSuccess(false), 3000);
    } catch {
      setError("Sync failed");
    } finally {
      setSyncLoading(false);
    }
  };

  return (
    <div className="flex flex-col pr-4 pl-6 space-y-6">
      {/* User Info */}
      {user && (
        <div className="text-sm text-gray-700">
          Logged in as <span className="font-medium">{user.email}</span>
        </div>
      )}

      {/* Success/Error Messages */}
      {syncSuccess && (
        <div className="bg-green-50 border border-green-200 rounded-lg p-4 flex items-center">
          <CheckCircle className="w-5 h-5 text-green-600 mr-3" />
          <span className="text-green-800">Sync completed successfully!</span>
        </div>
      )}

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-center">
          <AlertCircle className="w-5 h-5 text-red-600 mr-3" />
          <span className="text-red-800">{error}</span>
        </div>
      )}

      {/* Top Controls */}
      <div className="flex justify-end items-center space-x-3">
        <button
          onClick={() => handleConnectDrive("google")}
          disabled={loading}
          className="px-4 py-2 border border-gray-300 rounded-2xl text-sm font-medium hover:bg-gray-200 disabled:opacity-50"
        >
          Google Drive
        </button>

        <button
          onClick={() => handleConnectDrive("dropbox")}
          disabled={loading}
          className="px-4 py-2 border border-gray-300 rounded-2xl text-sm font-medium hover:bg-gray-200 disabled:opacity-50"
        >
          Dropbox
        </button>

        <button
          onClick={() => handleConnectDrive("onedrive")}
          disabled={loading}
          className="px-4 py-2 border border-gray-300 rounded-2xl text-sm font-medium hover:bg-gray-200 disabled:opacity-50"
        >
          OneDrive
        </button>

        <button
          onClick={handleSyncNow}
          disabled={syncLoading || !canSync(connectedDrives)}
          className="px-4 py-2 border border-gray-300 rounded-2xl text-sm font-medium text-black hover:bg-gray-200 disabled:opacity-50"
        >
          {syncLoading && <Loader2 className="w-4 h-4 mr-2 animate-spin inline" />}
          {syncLoading ? "Syncing..." : "Sync Now"}
        </button>
      </div>

      {/* Connected Drives */}
      <div className="bg-white rounded-xl shadow p-4">
        <h2 className="text-lg font-semibold mb-4">Connected Drives</h2>
        {connectedDrives.length > 0 ? (
          <ul className="space-y-3">
            {connectedDrives.map((drive) => (
              <li
                key={drive.id}
                className="flex items-center justify-between p-3 border rounded-lg bg-green-50 border-green-200"
              >
                <div className="flex items-center">
                  <HardDrive className="w-5 h-5 mr-3 text-green-600" />
                  <span className="font-medium text-gray-700">{drive.name}</span>
                </div>
                <CheckCircle className="w-5 h-5 text-green-600" />
              </li>
            ))}
          </ul>
        ) : (
          <div className="text-center py-8 text-gray-500">
            <HardDrive className="w-12 h-12 mx-auto mb-3 text-gray-300" />
            <p className="text-sm">No drives connected yet</p>
            <p className="text-xs text-gray-400 mt-1">Click the connect buttons above to get started</p>
          </div>
        )}
      </div>
    </div>
  );
}
