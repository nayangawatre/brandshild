import { useState, useEffect } from 'react';
import api from '../../api/axios';
import {
  HiUsers,
  HiShieldCheck,
  HiExclamationTriangle,
  HiClipboardDocumentList,
  HiCheckCircle,
  HiXCircle,
} from 'react-icons/hi2';
import toast, { Toaster } from 'react-hot-toast';

export default function AdminDashboard() {
  const [stats, setStats] = useState(null);
  const [users, setUsers] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('users'); // 'users' or 'logs'

  const fetchData = async () => {
    setLoading(true);
    try {
      const [statsRes, usersRes, logsRes] = await Promise.all([
        api.get('/admin/stats'),
        api.get('/admin/users'),
        api.get('/admin/audit-logs'),
      ]);
      setStats(statsRes.data);
      setUsers(usersRes.data);
      setAuditLogs(logsRes.data);
    } catch {
      toast.error('Failed to load admin data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleToggleUserStatus = async (user) => {
    const newStatus = !user.enabled;
    try {
      await api.put(`/admin/users/${user.id}/status`, { enabled: newStatus });
      setUsers((prev) =>
        prev.map((u) => (u.id === user.id ? { ...u, enabled: newStatus } : u))
      );
      toast.success(`User ${user.email} ${newStatus ? 'enabled' : 'disabled'}`);
    } catch {
      toast.error('Failed to update user status');
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <Toaster position="top-right" />

      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold flex items-center gap-2">
          <HiShieldCheck className="text-purple-400" />
          Admin & Analytics Console
        </h1>
        <p className="text-gray-400 text-sm mt-1">
          System-wide brand protection statistics, user controls, and audit trails
        </p>
      </div>

      {/* Metrics Row */}
      {stats && (
        <div className="grid grid-cols-2 lg:grid-cols-6 gap-4">
          <div className="bg-gray-800 border border-gray-700 rounded-xl p-4">
            <p className="text-xs text-gray-400">Total Users</p>
            <p className="text-2xl font-bold text-white mt-1">{stats.totalUsers}</p>
          </div>
          <div className="bg-gray-800 border border-gray-700 rounded-xl p-4">
            <p className="text-xs text-gray-400">Active Users</p>
            <p className="text-2xl font-bold text-emerald-400 mt-1">{stats.activeUsers}</p>
          </div>
          <div className="bg-gray-800 border border-gray-700 rounded-xl p-4">
            <p className="text-xs text-gray-400">Total Brands</p>
            <p className="text-2xl font-bold text-blue-400 mt-1">{stats.totalBrands}</p>
          </div>
          <div className="bg-gray-800 border border-gray-700 rounded-xl p-4">
            <p className="text-xs text-gray-400">Total Threats</p>
            <p className="text-2xl font-bold text-orange-400 mt-1">{stats.totalThreats}</p>
          </div>
          <div className="bg-gray-800 border border-gray-700 rounded-xl p-4">
            <p className="text-xs text-gray-400">Critical Threats</p>
            <p className="text-2xl font-bold text-red-400 mt-1">{stats.criticalThreats}</p>
          </div>
          <div className="bg-gray-800 border border-gray-700 rounded-xl p-4">
            <p className="text-xs text-gray-400">Pending Triage</p>
            <p className="text-2xl font-bold text-yellow-400 mt-1">{stats.pendingThreats}</p>
          </div>
        </div>
      )}

      {/* Tabs */}
      <div className="flex border-b border-gray-700 gap-4">
        <button
          onClick={() => setActiveTab('users')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === 'users'
              ? 'border-blue-500 text-blue-400'
              : 'border-transparent text-gray-400 hover:text-white'
          }`}
        >
          <HiUsers /> User Management ({users.length})
        </button>
        <button
          onClick={() => setActiveTab('logs')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === 'logs'
              ? 'border-blue-500 text-blue-400'
              : 'border-transparent text-gray-400 hover:text-white'
          }`}
        >
          <HiClipboardDocumentList /> Audit Trail ({auditLogs.length})
        </button>
      </div>

      {/* Users Tab */}
      {activeTab === 'users' && (
        <div className="bg-gray-800 border border-gray-700 rounded-xl overflow-hidden shadow-xl">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-gray-300">
              <thead className="bg-gray-750 text-gray-400 uppercase text-xs border-b border-gray-700">
                <tr>
                  <th className="px-6 py-4">User</th>
                  <th className="px-6 py-4">Role</th>
                  <th className="px-6 py-4">Status</th>
                  <th className="px-6 py-4">Joined</th>
                  <th className="px-6 py-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-700">
                {users.map((u) => (
                  <tr key={u.id} className="hover:bg-gray-750/70 transition">
                    <td className="px-6 py-4">
                      <div className="font-medium text-white">{u.fullName}</div>
                      <div className="text-xs text-gray-400">{u.email}</div>
                    </td>
                    <td className="px-6 py-4">
                      <span
                        className={`px-2.5 py-0.5 rounded text-xs font-semibold ${
                          u.role === 'ADMIN'
                            ? 'bg-purple-900/60 text-purple-300 border border-purple-800'
                            : 'bg-gray-700 text-gray-300'
                        }`}
                      >
                        {u.role}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      {u.enabled ? (
                        <span className="flex items-center gap-1.5 text-xs text-emerald-400">
                          <HiCheckCircle className="text-sm" /> Enabled
                        </span>
                      ) : (
                        <span className="flex items-center gap-1.5 text-xs text-red-400">
                          <HiXCircle className="text-sm" /> Disabled
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-xs text-gray-400">
                      {new Date(u.createdAt).toLocaleDateString()}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <button
                        onClick={() => handleToggleUserStatus(u)}
                        className={`px-3 py-1 text-xs rounded-lg font-medium transition ${
                          u.enabled
                            ? 'bg-red-900/40 text-red-400 hover:bg-red-900/60'
                            : 'bg-emerald-900/40 text-emerald-400 hover:bg-emerald-900/60'
                        }`}
                      >
                        {u.enabled ? 'Disable' : 'Enable'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Audit Logs Tab */}
      {activeTab === 'logs' && (
        <div className="bg-gray-800 border border-gray-700 rounded-xl overflow-hidden shadow-xl">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-gray-300">
              <thead className="bg-gray-750 text-gray-400 uppercase text-xs border-b border-gray-700">
                <tr>
                  <th className="px-6 py-4">Timestamp</th>
                  <th className="px-6 py-4">Action</th>
                  <th className="px-6 py-4">Resource</th>
                  <th className="px-6 py-4">Details</th>
                  <th className="px-6 py-4">IP Address</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-700">
                {auditLogs.length === 0 ? (
                  <tr>
                    <td colSpan="5" className="text-center py-8 text-gray-400">
                      No audit events recorded yet.
                    </td>
                  </tr>
                ) : (
                  auditLogs.map((log) => (
                    <tr key={log.id} className="hover:bg-gray-750/70 transition font-mono text-xs">
                      <td className="px-6 py-4 text-gray-400 whitespace-nowrap">
                        {new Date(log.timestamp).toLocaleString()}
                      </td>
                      <td className="px-6 py-4 text-blue-400 font-semibold">{log.action}</td>
                      <td className="px-6 py-4 text-gray-300">{log.resourceType || '—'}</td>
                      <td className="px-6 py-4 text-gray-300 break-all max-w-xs">{log.details}</td>
                      <td className="px-6 py-4 text-gray-400">{log.ipAddress || '127.0.0.1'}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
