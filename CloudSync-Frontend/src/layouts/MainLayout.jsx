// src/layouts/MainLayout.jsx
import Sidebar from "../components/Sidebar";
import Navbar from "../components/Navbar";

export default function MainLayout({ children }) {
  return (
    <div className="flex h-screen flex-col">
      {/* Top navbar spans full width */}
      <Navbar />

      {/* Row below navbar: sidebar + page content */}
      <div className="flex flex-1 min-h-0">
        {/* Sidebar fills the remaining height (below navbar) */}
        <Sidebar />

        {/* Page content area */}
        <main className="flex-1 overflow-auto p-4">
          {children}
        </main>
      </div>
    </div>
  );
}
