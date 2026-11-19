import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { HiShieldCheck, HiBell, HiArrowRightOnRectangle } from 'react-icons/hi2';
import { useState, useEffect } from 'react';
import api from '../api/axios';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    if (user) {
      api.get('/alerts/unread-count')
        .then((res) => setUnreadCount(res.data.count))
        .catch(() => {});
    }
  }, [user]);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="bg-gray-800 border-b border-gray-700 px-6 py-3 flex items-center justify-between">
      <Link to="/dashboard" className="flex items-center gap-2 text-white font-bold text-xl">
        <HiShieldCheck className="text-blue-400 text-2xl" />
        BrandShield
      </Link>

      <div className="flex items-center gap-4">
        <Link to="/alerts" className="relative text-gray-300 hover:text-white transition">
          <HiBell className="text-xl" />
          {unreadCount > 0 && (
            <span className="absolute -top-2 -right-2 bg-red-500 text-white text-xs rounded-full h-5 w-5 flex items-center justify-center">
              {unreadCount > 9 ? '9+' : unreadCount}
            </span>
          )}
        </Link>

        <div className="text-sm text-gray-300">
          <span className="text-white font-medium">{user?.fullName}</span>
          <span className="ml-2 px-2 py-0.5 rounded text-xs bg-gray-700 text-gray-300">
            {user?.role}
          </span>
        </div>

        <button
          onClick={handleLogout}
          className="text-gray-400 hover:text-red-400 transition"
          title="Logout"
        >
          <HiArrowRightOnRectangle className="text-xl" />
        </button>
      </div>
    </nav>
  );
}
