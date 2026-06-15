import { useState, useEffect } from "react";
import {
  Search,
  FolderOpen,
  AlertCircle,
  Loader2,
  File,
} from "lucide-react";
import { useAuth } from "../contexts/AuthContext";
import { listSyncFolderFiles, checkSyncFolder } from "../api/syncApi"; // ✅ unified API

export default function MyFolder() {
  const { hasConnectedDrives } = useAuth();

  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [activeFilter, setActiveFilter] = useState("All");
  const [syncFolderExists, setSyncFolderExists] = useState(false);

  const filters = ["All", "Word", "Excel", "PowerPoint", "OneNote", "PDF", "Images"];

  // Check if sync folder exists and load files
  useEffect(() => {
    const initializeFolder = async () => {
      if (!hasConnectedDrives()) {
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError(null);

        // Check if sync folder exists
        const syncFolderResponse = await checkSyncFolder();
        setSyncFolderExists(syncFolderResponse.exists);

        if (syncFolderResponse.exists) {
          // Load files from sync folder
          const filesResponse = await listSyncFolderFiles();
          setFiles(filesResponse.files || []);
        }
      } catch (err) {
        console.error("Error initializing folder:", err);
        setError(err.message || "Failed to load folder data");
        setSyncFolderExists(false);
      } finally {
        setLoading(false);
      }
    };

    initializeFolder();
  }, [hasConnectedDrives]);

  // Filter files based on search term and active filter
  const filteredFiles = files.filter((file) => {
    const matchesSearch = file.name?.toLowerCase().includes(searchTerm.toLowerCase());

    if (activeFilter === "All") return matchesSearch;

    const fileExtension = file.name?.split(".").pop()?.toLowerCase();
    const filterMap = {
      Word: ["doc", "docx"],
      Excel: ["xls", "xlsx"],
      PowerPoint: ["ppt", "pptx"],
      OneNote: ["one"],
      PDF: ["pdf"],
      Images: ["jpg", "jpeg", "png", "gif", "bmp", "svg"],
    };

    return matchesSearch && filterMap[activeFilter]?.includes(fileExtension);
  });

  const getFileIcon = (fileName) => {
    const extension = fileName?.split(".").pop()?.toLowerCase();
    const iconMap = {
      doc: "📄",
      docx: "📄",
      xls: "📊",
      xlsx: "📊",
      ppt: "📽️",
      pptx: "📽️",
      pdf: "📕",
      jpg: "🖼️",
      jpeg: "🖼️",
      png: "🖼️",
      gif: "🖼️",
      txt: "📝",
      zip: "📦",
      rar: "📦",
    };
    return iconMap[extension] || "📄";
  };

  const formatDate = (dateString) => {
    if (!dateString) return "-";
    const date = new Date(dateString);
    const now = new Date();
    const diffTime = Math.abs(now - date);
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays === 1) return "Today";
    if (diffDays === 2) return "Yesterday";
    if (diffDays <= 7) return `${diffDays} days ago`;

    return date.toLocaleDateString();
  };

  const formatFileSize = (bytes) => {
    if (!bytes) return "0 B";
    const sizes = ["B", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return `${Math.round((bytes / Math.pow(1024, i)) * 10) / 10} ${sizes[i]}`;
  };

  // No drives connected state
  if (!hasConnectedDrives()) {
    return (
      <div className="p-6 space-y-6">
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-8 text-center">
          <FolderOpen className="w-16 h-16 mx-auto mb-4 text-yellow-400" />
          <h2 className="text-xl font-semibold text-yellow-800 mb-2">
            No Drives Connected
          </h2>
          <p className="text-yellow-700 mb-4">
            Connect at least one cloud drive to start managing your synced files.
          </p>
          <p className="text-sm text-yellow-600">
            Go to Dashboard to connect Google Drive, Dropbox, or OneDrive.
          </p>
        </div>
      </div>
    );
  }

  // No sync folder detected state
  if (!loading && !syncFolderExists) {
    return (
      <div className="p-6 space-y-6">
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-8 text-center">
          <FolderOpen className="w-16 h-16 mx-auto mb-4 text-blue-400" />
          <h2 className="text-xl font-semibold text-blue-800 mb-2">
            No Sync Folder Detected
          </h2>
          <p className="text-blue-700 mb-4">
            Your sync folder hasn't been created yet. This happens automatically
            when you perform your first sync operation.
          </p>
          <p className="text-sm text-blue-600">
            Connect multiple drives and run a sync to create your sync folder.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* Top Controls */}
      <div className="flex justify-between items-center">
        {/* Left buttons - File type filters */}
        <div className="flex space-x-3">
          {filters.map((filter) => (
            <button
              key={filter}
              onClick={() => setActiveFilter(filter)}
              className={`px-4 py-2 border border-[#d1d1d1] rounded-full text-sm font-medium hover:bg-gray-200 transition-colors ${
                activeFilter === filter
                  ? "bg-blue-100 border-blue-300 text-blue-700"
                  : "text-black"
              }`}
            >
              {filter}
            </button>
          ))}
        </div>

        {/* Right search bar */}
        <div className="relative">
          <Search className="w-4 h-4 absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
          <input
            type="text"
            placeholder="Filter by name or person"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-10 px-4 py-2 border border-[#d1d1d1] rounded-full w-72 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          />
        </div>
      </div>

      {/* Error Message */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-center">
          <AlertCircle className="w-5 h-5 text-red-600 mr-3" />
          <span className="text-red-800">{error}</span>
        </div>
      )}

      {/* Loading State */}
      {loading && (
        <div className="flex justify-center items-center py-12">
          <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
          <span className="ml-3 text-gray-600">Loading your files...</span>
        </div>
      )}

      {/* Files Table */}
      {!loading && (
        <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
          {filteredFiles.length > 0 ? (
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-gray-600 border-b border-[#d1d1d1] bg-gray-50">
                  <th className="py-3 px-4">Name</th>
                  <th className="py-3 px-4">Size</th>
                  <th className="py-3 px-4">Modified</th>
                  <th className="py-3 px-4">Owner</th>
                  <th className="py-3 px-4">Source</th>
                </tr>
              </thead>
              <tbody>
                {filteredFiles.map((file) => (
                  <tr
                    key={file.id}
                    className="border-b border-[#d1d1d1] hover:bg-gray-50 transition-colors"
                  >
                    <td className="py-3 px-4 flex items-center space-x-3">
                      <span className="text-lg">{getFileIcon(file.name)}</span>
                      <span className="font-medium text-gray-900">{file.name}</span>
                    </td>
                    <td className="py-3 px-4 text-gray-600">
                      {formatFileSize(file.size)}
                    </td>
                    <td className="py-3 px-4 text-gray-600">
                      {formatDate(file.modifiedAt || file.modifiedTime)}
                    </td>
                    <td className="py-3 px-4 text-gray-600">
                      {file.owner || file.owners?.[0]?.displayName || "Unknown"}
                    </td>
                    <td className="py-3 px-4 text-gray-600">
                      {file.source || "Unknown"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <div className="text-center py-12">
              <File className="w-16 h-16 mx-auto mb-4 text-gray-300" />
              <h3 className="text-lg font-medium text-gray-600 mb-2">
                {searchTerm || activeFilter !== "All"
                  ? "No files match your filters"
                  : "No files in sync folder"}
              </h3>
              <p className="text-gray-500 text-sm">
                {searchTerm || activeFilter !== "All"
                  ? "Try adjusting your search or filter criteria"
                  : "Files will appear here after you sync content between your connected drives"}
              </p>
            </div>
          )}
        </div>
      )}

      {/* Stats Footer */}
      {!loading && filteredFiles.length > 0 && (
        <div className="flex justify-between items-center text-sm text-gray-500 px-2">
          <span>Showing {filteredFiles.length} of {files.length} files</span>
          <span>
            Total size:{" "}
            {formatFileSize(
              files.reduce((total, file) => total + (file.size || 0), 0)
            )}
          </span>
        </div>
      )}
    </div>
  );
}
