import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../../api/axios';
import { useAuth } from '../../context/AuthContext';
import { extractPageContent } from '../../utils/apiHelper';
import {
  HiShieldCheck,
  HiExclamationTriangle,
  HiBell,
  HiArrowTrendingUp,
} from 'react-icons/hi2';

function StatCard({ icon: Icon, label, value, color, to }) {
  const content = (
    <div className="bg-gray-800 border border-gray-700 rounded-xl p-6 hover:border-gray-600 transition">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-gray-400">{label}</p>
          <p className="text-3xl font-bold text-white mt-1">{value ?? '—'}</p>
        </div>
        <Icon className={`text-3xl ${color}`} />
      </div>
    </div>
  );
  return to ? <Link to={to}>{content}</Link> : content;
}

export default function Dashboard() {
  const { user } = useAuth();
  const [brands, setBrands] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [threatStats, setThreatStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [brandsRes, alertsRes] = await Promise.all([
          api.get('/brands'),
          api.get('/alerts/unread-count').catch(() => ({ data: { count: 0 } })),
        ]);
        const brandList = extractPageContent(brandsRes.data);
        setBrands(brandList);
        setUnreadCount(alertsRes.data?.count ?? 0);

        // Get threat stats for the first brand if exists
        if (brandList.length > 0) {
          try {
            const statsRes = await api.get(`/threats/stats/${brandList[0].id}`);
            setThreatStats(statsRes.data);
          } catch (e) {
            console.error('Failed to load initial threat stats:', e);
          }
        }
      } catch (err) {
        console.error('Dashboard data fetch error:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  const activeBrandsCount = Array.isArray(brands)
    ? brands.filter((b) => b.status === 'ACTIVE').length
    : 0;

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold">Welcome back, {user?.fullName || 'User'} 👋</h1>
        <p className="text-gray-400 mt-1">Here's your brand protection overview</p>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          icon={HiShieldCheck}
          label="Active Brands"
          value={activeBrandsCount}
          color="text-blue-400"
          to="/brands"
        />
        <StatCard
          icon={HiExclamationTriangle}
          label="Total Threats"
          value={threatStats?.totalThreats ?? 0}
          color="text-orange-400"
          to="/threats"
        />
        <StatCard
          icon={HiArrowTrendingUp}
          label="Avg Threat Score"
          value={threatStats?.averageScore != null ? Math.round(threatStats.averageScore) : 0}
          color="text-yellow-400"
        />
        <StatCard
          icon={HiBell}
          label="Unread Alerts"
          value={unreadCount}
          color="text-red-400"
          to="/alerts"
        />
      </div>

      {/* Brands quick list */}
      <div className="bg-gray-800 border border-gray-700 rounded-xl p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold">Your Brands</h2>
          <Link to="/brands" className="text-sm text-blue-400 hover:text-blue-300">
            View all →
          </Link>
        </div>

        {!brands || brands.length === 0 ? (
          <div className="text-center py-8 text-gray-400">
            <HiShieldCheck className="text-4xl mx-auto mb-2 text-gray-600" />
            <p>No brands yet.</p>
            <Link to="/brands/new" className="text-blue-400 hover:text-blue-300 text-sm mt-2 inline-block">
              + Add your first brand
            </Link>
          </div>
        ) : (
          <div className="space-y-3">
            {brands.slice(0, 5).map((brand) => (
              <Link
                key={brand.id}
                to={`/brands/${brand.id}`}
                className="flex items-center justify-between p-4 bg-gray-750 rounded-lg hover:bg-gray-700 transition border border-gray-700"
              >
                <div>
                  <p className="font-medium text-white">{brand.name}</p>
                  <p className="text-sm text-gray-400">{brand.description}</p>
                </div>
                <span
                  className={`px-3 py-1 rounded-full text-xs font-medium ${
                    brand.status === 'ACTIVE'
                      ? 'bg-green-900/50 text-green-400'
                      : brand.status === 'PAUSED'
                      ? 'bg-yellow-900/50 text-yellow-400'
                      : 'bg-gray-700 text-gray-400'
                  }`}
                >
                  {brand.status}
                </span>
              </Link>
            ))}
          </div>
        )}
      </div>

      {/* Threat severity breakdown */}
      {threatStats && (
        <div className="bg-gray-800 border border-gray-700 rounded-xl p-6">
          <h2 className="text-lg font-semibold mb-4">Threat Severity Breakdown</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {[
              { label: 'Critical', count: threatStats.criticalCount, color: 'text-red-400 bg-red-900/30' },
              { label: 'High', count: threatStats.highCount, color: 'text-orange-400 bg-orange-900/30' },
              { label: 'Medium', count: threatStats.mediumCount, color: 'text-yellow-400 bg-yellow-900/30' },
              { label: 'Low', count: threatStats.lowCount, color: 'text-green-400 bg-green-900/30' },
            ].map((item) => (
              <div key={item.label} className={`rounded-lg p-4 text-center ${item.color}`}>
                <p className="text-2xl font-bold">{item.count ?? 0}</p>
                <p className="text-sm">{item.label}</p>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
