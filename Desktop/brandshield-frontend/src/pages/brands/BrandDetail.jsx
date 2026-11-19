import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import api from '../../api/axios';
import { extractPageContent } from '../../utils/apiHelper';
import toast, { Toaster } from 'react-hot-toast';
import {
  HiPencil,
  HiTrash,
  HiMagnifyingGlass,
  HiExclamationTriangle,
} from 'react-icons/hi2';

export default function BrandDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [brand, setBrand] = useState(null);
  const [threats, setThreats] = useState([]);
  const [stats, setStats] = useState(null);
  const [scanning, setScanning] = useState(false);
  const [loading, setLoading] = useState(true);

  const fetchBrand = async () => {
    try {
      const [brandRes, threatsRes, statsRes] = await Promise.all([
        api.get(`/brands/${id}`),
        api.get(`/threats/brand/${id}`),
        api.get(`/threats/stats/${id}`),
      ]);
      setBrand(brandRes.data);
      setThreats(extractPageContent(threatsRes.data));
      setStats(statsRes.data);
    } catch {
      toast.error('Brand not found');
      navigate('/brands');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBrand();
  }, [id]);

  const handleScan = async () => {
    setScanning(true);
    try {
      const res = await api.post(`/threats/scan/${id}`);
      const detectedThreats = extractPageContent(res.data);
      toast.success(`Scan complete! Found ${detectedThreats.length} threat(s)`);
      fetchBrand();
    } catch {
      toast.error('Scan failed');
    } finally {
      setScanning(false);
    }
  };

  const handleDelete = async () => {
    if (!confirm('Are you sure you want to delete this brand? This action cannot be undone.')) return;
    try {
      await api.delete(`/brands/${id}`);
      toast.success('Brand deleted');
      navigate('/brands');
    } catch {
      toast.error('Delete failed');
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  if (!brand) return null;

  return (
    <div className="space-y-6">
      <Toaster position="top-right" />

      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold">{brand.name}</h1>
          <div className="flex flex-wrap items-center gap-3 text-sm text-gray-400 mt-1">
            {brand.officialWebsite && (
              <a
                href={brand.officialWebsite.startsWith('http') ? brand.officialWebsite : `https://${brand.officialWebsite}`}
                target="_blank"
                rel="noreferrer"
                className="text-blue-400 hover:underline"
              >
                {brand.officialWebsite} ↗
              </a>
            )}
            {brand.industry && <span>• {brand.industry}</span>}
          </div>
          {brand.description && <p className="text-gray-400 mt-2 text-sm">{brand.description}</p>}
        </div>
        <div className="flex gap-2">
          <button
            onClick={handleScan}
            disabled={scanning}
            className="flex items-center gap-2 bg-green-600 hover:bg-green-700 disabled:bg-green-800 text-white px-4 py-2 rounded-lg text-sm font-medium transition"
          >
            <HiMagnifyingGlass />
            {scanning ? 'Scanning...' : 'Scan Now'}
          </button>
          <Link
            to={`/brands/${id}/edit`}
            className="flex items-center gap-2 bg-gray-700 hover:bg-gray-600 text-white px-4 py-2 rounded-lg text-sm transition"
          >
            <HiPencil /> Edit
          </Link>
          <button
            onClick={handleDelete}
            className="flex items-center gap-2 bg-red-600/20 hover:bg-red-600/40 text-red-400 px-4 py-2 rounded-lg text-sm transition"
          >
            <HiTrash /> Delete
          </button>
        </div>
      </div>

      {/* Info cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-gray-800 border border-gray-700 rounded-xl p-5">
          <p className="text-sm text-gray-400">Status</p>
          <span
            className={`inline-block mt-1 px-3 py-1 rounded-full text-xs font-medium ${
              brand.status === 'ACTIVE'
                ? 'bg-green-900/50 text-green-400'
                : 'bg-yellow-900/50 text-yellow-400'
            }`}
          >
            {brand.status}
          </span>
        </div>
        <div className="bg-gray-800 border border-gray-700 rounded-xl p-5">
          <p className="text-sm text-gray-400">Total Threats</p>
          <p className="text-2xl font-bold mt-1">{stats?.totalThreats ?? 0}</p>
        </div>
        <div className="bg-gray-800 border border-gray-700 rounded-xl p-5">
          <p className="text-sm text-gray-400">Avg Threat Score</p>
          <p className="text-2xl font-bold mt-1">{stats?.averageScore != null ? Math.round(stats.averageScore) : 0}</p>
        </div>
      </div>

      {/* Keywords & Domains */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="bg-gray-800 border border-gray-700 rounded-xl p-5">
          <h3 className="text-sm font-medium text-gray-400 mb-3">Keywords</h3>
          <div className="flex flex-wrap gap-2">
            {brand.keywords?.length > 0 ? (
              brand.keywords.map((kw) => (
                <span key={kw} className="bg-blue-900/30 text-blue-400 text-xs px-3 py-1 rounded-full">
                  {kw}
                </span>
              ))
            ) : (
              <span className="text-gray-500 text-sm">No keywords added</span>
            )}
          </div>
        </div>
        <div className="bg-gray-800 border border-gray-700 rounded-xl p-5">
          <h3 className="text-sm font-medium text-gray-400 mb-3">Official Domains</h3>
          <div className="flex flex-wrap gap-2">
            {brand.officialDomains?.length > 0 ? (
              brand.officialDomains.map((d) => (
                <span key={d} className="bg-green-900/30 text-green-400 text-xs px-3 py-1 rounded-full">
                  {d}
                </span>
              ))
            ) : (
              <span className="text-gray-500 text-sm">No official domains added</span>
            )}
          </div>
        </div>
      </div>

      {/* Threats table */}
      <div className="bg-gray-800 border border-gray-700 rounded-xl p-6">
        <h2 className="text-lg font-semibold mb-4 flex items-center gap-2">
          <HiExclamationTriangle className="text-orange-400" />
          Detected Threats ({threats.length})
        </h2>

        {threats.length === 0 ? (
          <p className="text-gray-400 text-center py-8">No threats detected yet. Run a scan to check.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-gray-400 border-b border-gray-700">
                  <th className="pb-3 pr-4">Target</th>
                  <th className="pb-3 pr-4">Type</th>
                  <th className="pb-3 pr-4">Severity</th>
                  <th className="pb-3 pr-4">Score</th>
                  <th className="pb-3">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-700">
                {threats.map((threat) => (
                  <tr key={threat.id} className="hover:bg-gray-750">
                    <td className="py-3 pr-4 text-white font-mono text-xs">{threat.targetValue}</td>
                    <td className="py-3 pr-4 text-gray-300">{threat.type}</td>
                    <td className="py-3 pr-4">
                      <span
                        className={`px-2 py-0.5 rounded text-xs font-medium ${
                          threat.severity === 'CRITICAL'
                            ? 'bg-red-900/50 text-red-400'
                            : threat.severity === 'HIGH'
                            ? 'bg-orange-900/50 text-orange-400'
                            : threat.severity === 'MEDIUM'
                            ? 'bg-yellow-900/50 text-yellow-400'
                            : 'bg-green-900/50 text-green-400'
                        }`}
                      >
                        {threat.severity}
                      </span>
                    </td>
                    <td className="py-3 pr-4 text-white font-semibold">{threat.score}</td>
                    <td className="py-3 text-gray-400">{threat.status}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
