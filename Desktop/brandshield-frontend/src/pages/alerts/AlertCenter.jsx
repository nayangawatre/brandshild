import { useState, useEffect } from 'react';
import api from '../../api/axios';
import {
  HiBell,
  HiCheck,
  HiCheckCircle,
  HiTrash,
  HiEnvelope,
  HiDevicePhoneMobile,
  HiGlobeAlt,
} from 'react-icons/hi2';
import toast, { Toaster } from 'react-hot-toast';

export default function AlertCenter() {
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('ALL'); // 'ALL' or 'UNREAD'

  const fetchAlerts = async () => {
    setLoading(true);
    try {
      const res = await api.get('/alerts');
      // res.data can be page content or array
      const items = res.data.content ? res.data.content : (Array.isArray(res.data) ? res.data : []);
      setAlerts(items);
    } catch {
      toast.error('Failed to load alerts');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAlerts();
  }, []);

  const handleMarkRead = async (id) => {
    try {
      await api.patch(`/alerts/${id}/read`);
      setAlerts((prev) =>
        prev.map((a) => (a.id === id ? { ...a, status: 'READ' } : a))
      );
      toast.success('Alert marked as read');
    } catch {
      toast.error('Failed to update alert');
    }
  };

  const handleDismiss = async (id) => {
    try {
      await api.post(`/alerts/${id}/dismiss`);
      setAlerts((prev) =>
        prev.map((a) => (a.id === id ? { ...a, status: 'DISMISSED' } : a))
      );
      toast.success('Alert dismissed');
    } catch {
      toast.error('Failed to dismiss alert');
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await api.post('/alerts/read-all');
      setAlerts((prev) =>
        prev.map((a) => (a.status === 'UNREAD' ? { ...a, status: 'READ' } : a))
      );
      toast.success('All alerts marked as read');
    } catch {
      toast.error('Failed to mark all as read');
    }
  };

  const filteredAlerts = alerts.filter((a) => {
    if (filter === 'UNREAD') return a.status === 'UNREAD';
    return true;
  });

  const getChannelIcon = (channel) => {
    switch (channel) {
      case 'EMAIL':
        return <HiEnvelope className="text-blue-400" title="Email" />;
      case 'SMS':
        return <HiDevicePhoneMobile className="text-yellow-400" title="SMS" />;
      case 'WEBHOOK':
        return <HiGlobeAlt className="text-purple-400" title="Webhook" />;
      default:
        return <HiBell className="text-emerald-400" title="In-App" />;
    }
  };

  return (
    <div className="space-y-6">
      <Toaster position="top-right" />

      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold flex items-center gap-2">
            <HiBell className="text-red-400" />
            Alert Center
          </h1>
          <p className="text-gray-400 text-sm mt-1">
            Real-time security notifications dispatched for suspicious domain and brand threats
          </p>
        </div>

        <div className="flex items-center gap-3">
          <div className="bg-gray-800 border border-gray-700 rounded-lg p-1 flex">
            <button
              onClick={() => setFilter('ALL')}
              className={`px-3 py-1.5 text-xs font-medium rounded-md transition ${
                filter === 'ALL'
                  ? 'bg-blue-600 text-white'
                  : 'text-gray-400 hover:text-white'
              }`}
            >
              All Alerts
            </button>
            <button
              onClick={() => setFilter('UNREAD')}
              className={`px-3 py-1.5 text-xs font-medium rounded-md transition ${
                filter === 'UNREAD'
                  ? 'bg-blue-600 text-white'
                  : 'text-gray-400 hover:text-white'
              }`}
            >
              Unread Only
            </button>
          </div>

          <button
            onClick={handleMarkAllRead}
            className="flex items-center gap-2 bg-gray-800 hover:bg-gray-700 border border-gray-700 text-white px-3.5 py-1.5 rounded-lg text-xs font-medium transition"
          >
            <HiCheckCircle className="text-sm" /> Mark All as Read
          </button>
        </div>
      </div>

      {/* Alert Feed */}
      <div className="bg-gray-800 border border-gray-700 rounded-xl overflow-hidden shadow-xl">
        {loading ? (
          <div className="flex items-center justify-center h-48">
            <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-blue-500"></div>
          </div>
        ) : filteredAlerts.length === 0 ? (
          <div className="text-center py-16 text-gray-400">
            <HiBell className="text-5xl mx-auto mb-2 text-gray-600" />
            <p className="text-lg font-medium">No alerts found</p>
            <p className="text-sm mt-1">You're all caught up! High severity threats will trigger alerts here.</p>
          </div>
        ) : (
          <div className="divide-y divide-gray-700">
            {filteredAlerts.map((alert) => (
              <div
                key={alert.id}
                className={`p-5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 transition ${
                  alert.status === 'UNREAD' ? 'bg-gray-750/70 border-l-4 border-l-blue-500' : 'hover:bg-gray-750/30'
                }`}
              >
                <div className="flex items-start gap-4">
                  <div className="mt-1 p-2 bg-gray-700/80 rounded-lg text-lg">
                    {getChannelIcon(alert.channel)}
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h4 className="text-white font-medium text-sm">{alert.title}</h4>
                      <span
                        className={`text-[10px] px-2 py-0.5 rounded uppercase font-semibold ${
                          alert.status === 'UNREAD'
                            ? 'bg-blue-900/60 text-blue-300'
                            : alert.status === 'READ'
                            ? 'bg-gray-700 text-gray-400'
                            : 'bg-red-900/40 text-red-400'
                        }`}
                      >
                        {alert.status}
                      </span>
                    </div>
                    <p className="text-gray-300 text-sm mt-1">{alert.message}</p>
                    <p className="text-gray-500 text-xs mt-1">
                      {new Date(alert.createdAt).toLocaleString()}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2 self-end sm:self-center">
                  {alert.status === 'UNREAD' && (
                    <button
                      onClick={() => handleMarkRead(alert.id)}
                      className="p-2 bg-gray-700 hover:bg-gray-600 text-gray-200 rounded-lg text-xs flex items-center gap-1 transition"
                      title="Mark as Read"
                    >
                      <HiCheck className="text-sm" /> Read
                    </button>
                  )}
                  {alert.status !== 'DISMISSED' && (
                    <button
                      onClick={() => handleDismiss(alert.id)}
                      className="p-2 bg-gray-700 hover:bg-red-900/50 hover:text-red-400 text-gray-400 rounded-lg text-xs flex items-center gap-1 transition"
                      title="Dismiss Alert"
                    >
                      <HiTrash className="text-sm" /> Dismiss
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
