import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';
import ErrorBoundary from './components/ErrorBoundary';

// Pages
import Login from './pages/auth/Login';
import Register from './pages/auth/Register';
import Dashboard from './pages/dashboard/Dashboard';
import BrandList from './pages/brands/BrandList';
import BrandForm from './pages/brands/BrandForm';
import BrandDetail from './pages/brands/BrandDetail';
import ThreatList from './pages/threats/ThreatList';
import AlertCenter from './pages/alerts/AlertCenter';
import AdminDashboard from './pages/admin/AdminDashboard';

export default function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            {/* Public Routes */}
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />

            {/* Protected Application Routes */}
            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <Layout />
                </ProtectedRoute>
              }
            >
              <Route index element={<Navigate to="/dashboard" replace />} />
              <Route path="dashboard" element={<Dashboard />} />
              <Route path="brands" element={<BrandList />} />
              <Route path="brands/new" element={<BrandForm />} />
              <Route path="brands/:id" element={<BrandDetail />} />
              <Route path="brands/:id/edit" element={<BrandForm />} />
              <Route path="threats" element={<ThreatList />} />
              <Route path="alerts" element={<AlertCenter />} />

              {/* Admin Console Route */}
              <Route
                path="admin"
                element={
                  <ProtectedRoute adminOnly={true}>
                    <AdminDashboard />
                  </ProtectedRoute>
                }
              />
            </Route>

            {/* Catch-all redirect */}
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </ErrorBoundary>
  );
}
