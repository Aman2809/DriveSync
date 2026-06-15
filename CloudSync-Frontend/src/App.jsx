// src/App.jsx
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import MainLayout from "./layouts/MainLayout";
import Dashboard from "./pages/Dashboard";
import MyFolder from "./pages/MyFolder";

function App() {
  return (
    <Router>
      <MainLayout>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/my-folder" element={<MyFolder />} />
          <Route path="/drive/:driveId" element={<DriveView />} />
        </Routes>
      </MainLayout>
    </Router>
  );
}

// ✅ Placeholder component for individual drive views
function DriveView() {
  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-4">Drive View</h1>
      <p>Individual drive view component will be implemented here.</p>
    </div>
  );
}

export default App;
