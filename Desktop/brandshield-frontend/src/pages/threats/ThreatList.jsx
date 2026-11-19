import { useState, useEffect } from 'react';
import api from '../../api/axios';
import { extractPageContent } from '../../utils/apiHelper';
import {
  HiExclamationTriangle,
  HiMagnifyingGlass,
  HiAdjustmentsHorizontal,
  HiArrowPath,
  HiEye,
  HiXMark,
  HiGlobeAlt,
  HiShieldCheck,
  HiDocumentText,
  HiPhoto,
} from 'react-icons/hi2';
import toast, { Toaster } from 'react-hot-toast';

export default function ThreatList() {
  const [brands, setBrands] = useState([]);
  const [selectedBrandId, setSelectedBrandId] = useState('');
  const [threats, setThreats] = useState([]);
  const [loading, setLoading] = useState(true);
  const [analyzing, setAnalyzing] = useState(false);
  const [targetUrl, setTargetUrl] = useState('');
  const [severityFilter, setSeverityFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');

  // Intelligence Modal State
  const [intelModalOpen, setIntelModalOpen] = useState(false);
  const [intelData, setIntelData] = useState(null);
  const [intelLoading, setIntelLoading] = useState(false);

  // Load brands on mount
  useEffect(() => {
    api.get('/brands')
      .then((res) => {
        const brandList = extractPageContent(res.data);
        setBrands(brandList);
        if (brandList.length > 0) {
          setSelectedBrandId(brandList[0].id);
        } else {
          setLoading(false);
        }
      })
      .catch(() => {
        toast.error('Failed to load brands');
        setLoading(false);
      });
  }, []);

  // Load threats whenever selected brand changes
  const fetchThreats = async () => {
    if (!selectedBrandId) {
      setThreats([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const res = await api.get(`/threats/brand/${selectedBrandId}`);
      setThreats(extractPageContent(res.data));
    } catch {
      toast.error('Failed to load threats');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (selectedBrandId) {
      fetchThreats();
    }
  }, [selectedBrandId]);

  // On-demand analyze
  const handleAnalyze = async (e) => {
    e.preventDefault();
    if (!targetUrl.trim() || !selectedBrandId) return;

    setAnalyzing(true);
    try {
      const res = await api.post('/threats/analyze', {
        brandId: selectedBrandId,
        targetValue: targetUrl.trim(),
      });
      toast.success(`Analysis complete: ${res.data.severity} threat detected (Score: ${res.data.score})`);
      setTargetUrl('');
      fetchThreats();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Threat analysis failed');
    } finally {
      setAnalyzing(false);
    }
  };

  // Update status (triage)
  const handleStatusChange = async (threatId, newStatus) => {
    try {
      await api.patch(`/threats/${threatId}/status`, {
        status: newStatus,
        notes: `Updated status to ${newStatus}`,
      });
      toast.success(`Status updated to ${newStatus}`);
      fetchThreats();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to update threat status');
    }
  };

  // Inspect Domain Intelligence (DNS, RDAP, CT, Jsoup, Playwright)
  const handleInspectIntelligence = async (domain) => {
    setIntelLoading(true);
    setIntelModalOpen(true);
    setIntelData(null);
    try {
      const res = await api.get(`/threats/intelligence?brandId=${selectedBrandId}&domain=${encodeURIComponent(domain)}`);
      setIntelData(res.data);
    } catch (err) {
      toast.error('Failed to retrieve intelligence report');
    } finally {
      setIntelLoading(false);
    }
  };

  // Filtered threats
  const filteredThreats = Array.isArray(threats)
    ? threats.filter((t) => {
        const matchSev = severityFilter === 'ALL' || t.severity === severityFilter;
        const matchStat = statusFilter === 'ALL' || t.status === statusFilter;
        return matchSev && matchStat;
      })
    : [];

  return (
    <div className="space-y-6">
      <Toaster position="top-right" />

      {/* Header and Brand Selection */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold flex items-center gap-2">
            <HiExclamationTriangle className="text-orange-400" />
            Threat Center
          </h1>
          <p className="text-gray-400 text-sm mt-1">
            Monitor, analyze, and triage domain impersonation, DNS, RDAP, and SSL certificates
          </p>
        </div>

        <div className="flex items-center gap-3">
          <label className="text-sm text-gray-300 font-medium">Brand:</label>
          <select
            value={selectedBrandId}
            onChange={(e) => setSelectedBrandId(e.target.value)}
            className="bg-gray-800 border border-gray-700 text-white text-sm rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:outline-none"
          >
            {brands.map((b) => (
              <option key={b.id} value={b.id}>
                {b.name}
              </option>
            ))}
          </select>

          <button
            onClick={fetchThreats}
            className="p-2 bg-gray-800 hover:bg-gray-700 border border-gray-700 text-gray-300 rounded-lg transition"
            title="Refresh threats"
          >
            <HiArrowPath className={loading ? 'animate-spin' : ''} />
          </button>
        </div>
      </div>

      {/* Quick Analyze Form */}
      <div className="bg-gray-800 border border-gray-700 rounded-xl p-5 shadow-lg">
        <h2 className="text-md font-semibold text-white mb-2 flex items-center gap-2">
          <HiMagnifyingGlass className="text-blue-400" />
          On-Demand Threat Inspection
        </h2>
        <form onSubmit={handleAnalyze} className="flex flex-col sm:flex-row gap-3 mt-3">
          <input
            type="text"
            value={targetUrl}
            onChange={(e) => setTargetUrl(e.target.value)}
            placeholder="Enter suspicious domain or URL (e.g. apple-login-security.xyz)"
            className="flex-1 bg-gray-700 border border-gray-600 rounded-lg px-4 py-2.5 text-white placeholder-gray-400 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            required
          />
          <button
            type="submit"
            disabled={analyzing || !selectedBrandId}
            className="bg-blue-600 hover:bg-blue-700 disabled:bg-blue-800 text-white px-5 py-2.5 rounded-lg text-sm font-medium transition flex items-center justify-center gap-2"
          >
            {analyzing ? 'Inspecting...' : 'Analyze Target'}
          </button>
        </form>
      </div>

      {/* Filter Bar */}
      <div className="flex flex-wrap items-center justify-between gap-4 bg-gray-800/60 p-4 rounded-xl border border-gray-700">
        <div className="flex items-center gap-2 text-sm text-gray-400">
          <HiAdjustmentsHorizontal className="text-lg" />
          <span>Filters:</span>
        </div>

        <div className="flex flex-wrap gap-4 text-sm">
          <div className="flex items-center gap-2">
            <span className="text-gray-400">Severity:</span>
            <select
              value={severityFilter}
              onChange={(e) => setSeverityFilter(e.target.value)}
              className="bg-gray-700 border border-gray-600 text-white rounded-md px-2.5 py-1 text-xs focus:ring-1 focus:ring-blue-500"
            >
              <option value="ALL">All Severities</option>
              <option value="CRITICAL">Critical</option>
              <option value="HIGH">High</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
            </select>
          </div>

          <div className="flex items-center gap-2">
            <span className="text-gray-400">Status:</span>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="bg-gray-700 border border-gray-600 text-white rounded-md px-2.5 py-1 text-xs focus:ring-1 focus:ring-blue-500"
            >
              <option value="ALL">All Statuses</option>
              <option value="DETECTED">Detected</option>
              <option value="INVESTIGATING">Investigating</option>
              <option value="CONFIRMED">Confirmed</option>
              <option value="FALSE_POSITIVE">False Positive</option>
              <option value="MITIGATED">Mitigated</option>
            </select>
          </div>
        </div>
      </div>

      {/* Threats Table */}
      <div className="bg-gray-800 border border-gray-700 rounded-xl overflow-hidden shadow-xl">
        {loading ? (
          <div className="flex items-center justify-center h-48">
            <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-blue-500"></div>
          </div>
        ) : filteredThreats.length === 0 ? (
          <div className="text-center py-16 text-gray-400">
            <HiExclamationTriangle className="text-5xl mx-auto mb-2 text-gray-600" />
            <p className="text-lg font-medium">No threats found</p>
            <p className="text-sm mt-1">Try analyzing a target or scanning your brand.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-gray-300">
              <thead className="bg-gray-750 text-gray-400 uppercase text-xs border-b border-gray-700">
                <tr>
                  <th className="px-6 py-4">Target Value</th>
                  <th className="px-6 py-4">Type</th>
                  <th className="px-6 py-4">Score</th>
                  <th className="px-6 py-4">Severity</th>
                  <th className="px-6 py-4">Status / Triage</th>
                  <th className="px-6 py-4 text-center">Intelligence</th>
                  <th className="px-6 py-4">Detected</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-700">
                {filteredThreats.map((threat) => (
                  <tr key={threat.id} className="hover:bg-gray-750/70 transition">
                    <td className="px-6 py-4 font-mono font-medium text-white break-all max-w-xs">
                      {threat.targetValue}
                    </td>
                    <td className="px-6 py-4 text-xs">
                      <span className="bg-gray-700 text-gray-300 px-2.5 py-1 rounded">
                        {threat.type}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-white">{threat.score}</span>
                        <div className="w-16 bg-gray-700 rounded-full h-1.5 overflow-hidden">
                          <div
                            className={`h-full ${
                              threat.score >= 80
                                ? 'bg-red-500'
                                : threat.score >= 60
                                ? 'bg-orange-500'
                                : threat.score >= 40
                                ? 'bg-yellow-500'
                                : 'bg-green-500'
                            }`}
                            style={{ width: `${Math.min(100, threat.score)}%` }}
                          />
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <span
                        className={`px-2.5 py-1 rounded-full text-xs font-semibold ${
                          threat.severity === 'CRITICAL'
                            ? 'bg-red-900/60 text-red-400 border border-red-800'
                            : threat.severity === 'HIGH'
                            ? 'bg-orange-900/60 text-orange-400 border border-orange-800'
                            : threat.severity === 'MEDIUM'
                            ? 'bg-yellow-900/60 text-yellow-400 border border-yellow-800'
                            : 'bg-green-900/60 text-green-400 border border-green-800'
                        }`}
                      >
                        {threat.severity}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <select
                        value={threat.status}
                        onChange={(e) => handleStatusChange(threat.id, e.target.value)}
                        className="bg-gray-700 text-xs text-white rounded-md px-2.5 py-1 border border-gray-600 focus:outline-none focus:ring-1 focus:ring-blue-500 cursor-pointer"
                      >
                        <option value="DETECTED">DETECTED</option>
                        <option value="INVESTIGATING">INVESTIGATING</option>
                        <option value="CONFIRMED">CONFIRMED</option>
                        <option value="FALSE_POSITIVE">FALSE POSITIVE</option>
                        <option value="MITIGATED">MITIGATED</option>
                      </select>
                    </td>
                    <td className="px-6 py-4 text-center">
                      <button
                        onClick={() => handleInspectIntelligence(threat.targetValue)}
                        className="px-3 py-1.5 bg-blue-600/30 hover:bg-blue-600/50 text-blue-300 border border-blue-500/40 rounded-lg text-xs font-medium inline-flex items-center gap-1.5 transition"
                        title="View DNS, RDAP, CT and HTML Analysis"
                      >
                        <HiEye /> Inspect
                      </button>
                    </td>
                    <td className="px-6 py-4 text-xs text-gray-400 whitespace-nowrap">
                      {new Date(threat.detectedAt).toLocaleDateString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Intelligence Modal */}
      {intelModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm overflow-y-auto">
          <div className="bg-gray-800 border border-gray-700 rounded-2xl w-full max-w-4xl max-h-[90vh] overflow-y-auto shadow-2xl p-6 space-y-6">
            <div className="flex items-center justify-between border-b border-gray-700 pb-4">
              <div className="flex items-center gap-3">
                <HiGlobeAlt className="text-2xl text-blue-400" />
                <div>
                  <h3 className="text-lg font-bold text-white">Domain Intelligence Report</h3>
                  <p className="text-xs text-gray-400 font-mono">{intelData?.domain || 'Loading...'}</p>
                </div>
              </div>
              <button
                onClick={() => setIntelModalOpen(false)}
                className="text-gray-400 hover:text-white p-1 rounded-lg text-xl"
              >
                <HiXMark />
              </button>
            </div>

            {intelLoading ? (
              <div className="flex flex-col items-center justify-center py-16 gap-3">
                <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-blue-500"></div>
                <p className="text-sm text-gray-400">Querying DNS, RDAP Registry, Certificate Transparency & Jsoup Content...</p>
              </div>
            ) : intelData ? (
              <div className="space-y-6 text-sm">
                {/* DNS & RDAP Grid */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {/* DNS */}
                  <div className="bg-gray-750 border border-gray-700 rounded-xl p-4">
                    <h4 className="font-semibold text-white flex items-center gap-2 mb-3">
                      <HiShieldCheck className="text-blue-400" /> DNS Verification
                    </h4>
                    <div className="space-y-2 text-xs">
                      <div>
                        <span className="text-gray-400">Resolution Status: </span>
                        <span className={intelData.dns?.isResolving ? 'text-emerald-400 font-semibold' : 'text-orange-400 font-semibold'}>
                          {intelData.dns?.status}
                        </span>
                      </div>
                      <div>
                        <span className="text-gray-400">IP Addresses: </span>
                        <span className="text-gray-200 font-mono">
                          {intelData.dns?.ipAddresses?.length > 0 ? intelData.dns.ipAddresses.join(', ') : 'None'}
                        </span>
                      </div>
                      <div>
                        <span className="text-gray-400">Mail (MX) Records: </span>
                        <span className="text-gray-200 font-mono">
                          {intelData.dns?.mxRecords?.length > 0 ? intelData.dns.mxRecords.join(', ') : 'No MX configured'}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* RDAP */}
                  <div className="bg-gray-750 border border-gray-700 rounded-xl p-4">
                    <h4 className="font-semibold text-white flex items-center gap-2 mb-3">
                      <HiDocumentText className="text-purple-400" /> RDAP Registration Info
                    </h4>
                    <div className="space-y-2 text-xs">
                      <div>
                        <span className="text-gray-400">Registrar: </span>
                        <span className="text-white font-medium">{intelData.rdap?.registrar}</span>
                      </div>
                      <div>
                        <span className="text-gray-400">Created: </span>
                        <span className="text-gray-300">{intelData.rdap?.createdDate}</span>
                      </div>
                      <div>
                        <span className="text-gray-400">Expires: </span>
                        <span className="text-gray-300">{intelData.rdap?.expirationDate}</span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Website Analysis (Jsoup) */}
                <div className="bg-gray-750 border border-gray-700 rounded-xl p-4">
                  <h4 className="font-semibold text-white flex items-center gap-2 mb-3">
                    <HiGlobeAlt className="text-emerald-400" /> Jsoup Website & Content Analysis
                  </h4>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
                    <div>
                      <p className="text-gray-400">Website Title:</p>
                      <p className="text-white font-medium mt-0.5">{intelData.contentAnalysis?.title || 'None / Not Accessible'}</p>
                    </div>
                    <div>
                      <p className="text-gray-400">Phishing Form Detection:</p>
                      <p className={intelData.contentAnalysis?.hasPasswordField ? 'text-red-400 font-bold mt-0.5' : 'text-emerald-400 mt-0.5'}>
                        {intelData.contentAnalysis?.hasPasswordField ? '⚠️ Password input form detected' : '✓ No password inputs observed'}
                      </p>
                    </div>
                    <div>
                      <p className="text-gray-400">Text Similarity with Official Brand:</p>
                      <p className="text-blue-400 font-semibold mt-0.5">
                        {Math.round((intelData.contentAnalysis?.textSimilarityScore || 0) * 100)}% Match
                      </p>
                    </div>
                    <div>
                      <p className="text-gray-400">Brand Name Mention:</p>
                      <p className="text-gray-200 mt-0.5">{intelData.contentAnalysis?.detectedBrandMention}</p>
                    </div>
                  </div>
                </div>

                {/* Playwright Rendering Snapshot */}
                {intelData.rendering && (
                  <div className="bg-gray-750 border border-gray-700 rounded-xl p-4">
                    <h4 className="font-semibold text-white flex items-center gap-2 mb-3">
                      <HiPhoto className="text-yellow-400" /> Playwright Browser Rendering Pipeline
                    </h4>
                    <p className="text-xs text-gray-400 mb-3">
                      Render Engine: <span className="text-white font-medium">{intelData.rendering.renderEngine}</span> • {intelData.rendering.notes}
                    </p>
                    {intelData.rendering.screenshotPreviewUrl && (
                      <div className="border border-gray-700 rounded-lg overflow-hidden bg-gray-900 max-h-56 flex items-center justify-center">
                        <img
                          src={intelData.rendering.screenshotPreviewUrl}
                          alt="Rendered Website Screenshot"
                          className="max-h-56 object-cover w-full"
                          onError={(e) => { e.target.style.display = 'none'; }}
                        />
                      </div>
                    )}
                  </div>
                )}

                {/* Certificate Transparency */}
                <div className="bg-gray-750 border border-gray-700 rounded-xl p-4">
                  <h4 className="font-semibold text-white flex items-center gap-2 mb-3">
                    <HiShieldCheck className="text-teal-400" /> Certificate Transparency (CT) Log Discoveries
                  </h4>
                  {intelData.certificateTransparencyFindings?.length > 0 ? (
                    <div className="space-y-2 max-h-36 overflow-y-auto">
                      {intelData.certificateTransparencyFindings.map((ct, idx) => (
                        <div key={idx} className="flex justify-between items-center text-xs bg-gray-700/60 p-2 rounded">
                          <span className="font-mono text-white">{ct.domain}</span>
                          <span className="text-gray-400">{ct.issuerName?.split(',')[0]}</span>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-xs text-gray-400">No external CT certificate registrations discovered for this brand query.</p>
                  )}
                </div>
              </div>
            ) : null}
          </div>
        </div>
      )}
    </div>
  );
}
