// src/components/Navbar.jsx
import { Search, HelpCircle, Settings, Share2, Crown } from "lucide-react";

export default function Navbar() {
  return (
    <div className="w-full h-14   flex items-center gap-4 px-4">
      {/* Left: App name */}
      <div className="font-semibold text-[16px] text-black">CloudSync</div>

      {/* Middle: Search */}
      <div className="flex-1 flex justify-center">
        <div className="relative w-full max-w-lg">
          <Search className="absolute left-3 top-2.5 text-gray-400 w-5 h-5" />
          <input
            type="text"
            placeholder="Search everything"
            className="w-full pl-10 pr-2 py-2 border bg-white border-gray-300 rounded-full text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
      </div>


      {/* Right: actions */}
      <div className="flex items-center space-x-3">
        <button className="p-2 rounded-full hover:bg-gray-100">
          <Crown className="w-5 h-5" />
        </button>
        <button className="p-2 rounded-full hover:bg-gray-100">
          <Share2 className="w-5 h-5" />
        </button>
        <button className="p-2 rounded-full hover:bg-gray-100">
          <Settings className="w-5 h-5" />
        </button>
        <button className="p-2 rounded-full hover:bg-gray-100">
          <HelpCircle className="w-5 h-5" />
        </button>
        <div className="ml-1 w-9 h-9 flex items-center justify-center rounded-full bg-blue-600 text-white font-semibold cursor-pointer">
          AJ
        </div>
      </div>
    </div>
  );
}
