import { Link } from "react-router-dom";
import { useState, useEffect } from "react";
import {
  Home,
  Folder,
  HardDrive,
  PlusCircle,
  RefreshCw,
  Loader2
} from "lucide-react";
import { syncBidirectionalAuto, canSync } from "../api/syncApi";
import { fetchConnectedDrives } from "../api/driveApi";
import { getCurrentUser } from "../api/oauthApi";

export default function Sidebar() {
  const [connectedDrives, setConnectedDrives] = useState([]);
  const [loading, setLoading] = useState(false);
  const [syncLoading, setSyncLoading] = useState(false);
  const [error, setError] = useState(null);
  const [syncError, setSyncError] = useState(null);

  useEffect(() => {
    loadDrives();
  }, []);

  const loadDrives = async () => {
    try {
      const user = await getCurrentUser();
      if (!user) {
        setConnectedDrives([]);
        return;
      }
      const drives = await fetchConnectedDrives(user.id);
      setConnectedDrives(drives); // [{id: 1, name: "Google Drive", type: "google"}, ...]
    } catch (err) {
      setError("Failed to fetch connected drives");
    }
  };

  const handleSyncNow = async () => {
    if (!canSync(connectedDrives)) {
      setSyncError("Please connect at least two drives to sync");
      return;
    }
    try {
      setSyncLoading(true);
      setSyncError(null);
      await syncBidirectionalAuto();
      console.log("Sync completed successfully");
    } catch (err) {
      setSyncError("Sync failed");
    } finally {
      setSyncLoading(false);
    }
  };

  return (
    <aside className="w-68 border-r flex flex-col overflow-y-auto pt-2">
      {/* Top "New" Button */}
      <div className="p-3">
        <button className="flex items-center justify-center w-fit px-3 py-2 text-sm font-medium text-white rounded-2xl [background:linear-gradient(128.84deg,#0f6cbd_20.46%,#3c45ab_72.3%)]">
          <PlusCircle className="w-4 h-4 mr-2" />
          Create or Upload
        </button>
      </div>

      {/* User Name */}
      <div className="p-3">
        <button className="flex w-full px-3 text-sm font-medium text-black hover:bg-blue-700">
          Aman Jha
        </button>
      </div>

      {/* Navigation links */}
      <nav className="flex-1 px-2 py-1 space-y-2">
        <Link
          to="/"
          className="flex text-sm items-center px-3 py-1 text-black hover:bg-gray-200 rounded-md"
        >
          <Home className="w-5 h-4 mr-3" />
          Dashboard
        </Link>

        <Link
          to="/my-folder"
          className="flex text-sm items-center px-3 py-1 text-black hover:bg-gray-200 rounded-md"
        >
          <Folder className="w-5 h-4 mr-3" />
          My Folder
        </Link>

        {/* Connected Drives Section */}
        <div className="mt-6">
          <h2 className="px-3 text-sm font-medium text-black uppercase tracking-wide">
            Connected Drives
          </h2>

          <div className="mt-2 space-y-1">
            {connectedDrives.length > 0 ? (
              connectedDrives.map((drive) => (
                <Link
                  key={drive.id}
                  to={`/drive/${drive.type}`} // e.g. /drive/google
                  className="flex text-sm items-center px-3 py-2 text-black hover:bg-gray-200 rounded-md"
                >
                  <HardDrive className="w-5 h-4 mr-3 text-green-600" />
                  {drive.name}
                </Link>
              ))
            ) : (
              <div className="px-3 py-2 text-xs text-gray-500 italic">
                No drives connected yet
              </div>
            )}
          </div>
        </div>
      </nav>

      {/* Footer actions pinned to bottom */}
      <div className="px-2 py-4 space-y-2">
        {/* Sync Now Button */}
        <button
          onClick={handleSyncNow}
          disabled={syncLoading || !canSync(connectedDrives)}
          className="flex text-sm items-center w-full px-3 py-2 text-green-600 hover:bg-green-50 rounded-md disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {syncLoading ? (
            <Loader2 className="w-5 h-5 mr-3 animate-spin" />
          ) : (
            <RefreshCw className="w-5 h-5 mr-3" />
          )}
          {syncLoading ? "Syncing..." : "Sync Now"}
        </button>

        {/* Error Messages */}
        {error && (
          <div className="px-3 py-2 text-xs text-red-600 bg-red-50 rounded-md">
            {error}
          </div>
        )}
        {syncError && (
          <div className="px-3 py-2 text-xs text-red-600 bg-red-50 rounded-md">
            {syncError}
          </div>
        )}
      </div>
    </aside>
  );
}
