import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  HiOutlineHome,
  HiOutlineShieldCheck,
  HiOutlineExclamationTriangle,
  HiOutlineBell,
  HiOutlineCog6Tooth,
} from 'react-icons/hi2';

const navItems = [
  { to: '/dashboard', label: 'Dashboard', icon: HiOutlineHome },
  { to: '/brands', label: 'Brands', icon: HiOutlineShieldCheck },
  { to: '/threats', label: 'Threats', icon: HiOutlineExclamationTriangle },
  { to: '/alerts', label: 'Alerts', icon: HiOutlineBell },
];

const adminItems = [
  { to: '/admin', label: 'Admin Console', icon: HiOutlineCog6Tooth },
];

export default function Sidebar() {
  const { user } = useAuth();
  const items = user?.role === 'ADMIN' ? [...navItems, ...adminItems] : navItems;

  return (
    <aside className="w-64 bg-gray-800 border-r border-gray-700 min-h-screen p-4">
      <nav className="space-y-1">
        {items.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              `flex items-center gap-3 px-4 py-2.5 rounded-lg text-sm font-medium transition ${
                isActive
                  ? 'bg-blue-600 text-white'
                  : 'text-gray-300 hover:bg-gray-700 hover:text-white'
              }`
            }
          >
            <item.icon className="text-lg" />
            {item.label}
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}
