import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../../api/axios';
import { formatApiErrorMessage } from '../../utils/apiHelper';
import toast, { Toaster } from 'react-hot-toast';

export default function BrandForm() {
  const { id } = useParams();
  const isEditing = Boolean(id);
  const navigate = useNavigate();

  const [name, setName] = useState('');
  const [officialWebsite, setOfficialWebsite] = useState('');
  const [industry, setIndustry] = useState('');
  const [description, setDescription] = useState('');
  const [keywords, setKeywords] = useState('');
  const [officialDomains, setOfficialDomains] = useState('');
  const [status, setStatus] = useState('ACTIVE');
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(isEditing);

  useEffect(() => {
    if (isEditing) {
      api.get(`/brands/${id}`)
        .then((res) => {
          const b = res.data;
          setName(b.name || '');
          setOfficialWebsite(b.officialWebsite || '');
          setIndustry(b.industry || '');
          setDescription(b.description || '');
          setStatus(b.status || 'ACTIVE');
          setKeywords(b.keywords?.join(', ') || '');
          setOfficialDomains(b.officialDomains?.join(', ') || '');
        })
        .catch((err) => {
          toast.error(formatApiErrorMessage(err, 'Brand not found'));
          navigate('/brands');
        })
        .finally(() => setLoading(false));
    }
  }, [id]);

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!officialWebsite.trim()) {
      toast.error('Official website is required');
      return;
    }

    setSubmitting(true);

    const parsedKeywords = keywords
      .split(',')
      .map((k) => k.trim())
      .filter(Boolean);

    const parsedDomains = officialDomains
      .split(',')
      .map((d) => d.trim())
      .filter(Boolean);

    const payload = isEditing
      ? {
          name: name.trim(),
          officialWebsite: officialWebsite.trim(),
          industry: industry.trim() || null,
          description: description.trim() || null,
          status,
        }
      : {
          name: name.trim(),
          officialWebsite: officialWebsite.trim(),
          industry: industry.trim() || null,
          description: description.trim() || null,
          keywords: parsedKeywords,
          officialDomains: parsedDomains,
        };

    try {
      if (isEditing) {
        await api.put(`/brands/${id}`, payload);
        toast.success('Brand updated successfully');
      } else {
        await api.post('/brands', payload);
        toast.success('Brand created successfully');
      }
      navigate('/brands');
    } catch (err) {
      toast.error(formatApiErrorMessage(err, 'Failed to save brand'));
    } finally {
      setSubmitting(false);
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
    <div className="max-w-2xl mx-auto">
      <Toaster position="top-right" />
      <h1 className="text-2xl font-bold mb-6">{isEditing ? 'Edit Brand' : 'Add New Brand'}</h1>

      <form onSubmit={handleSubmit} className="bg-gray-800 border border-gray-700 rounded-xl p-8 space-y-5">
        <div>
          <label className="block text-sm font-medium text-gray-300 mb-1">Brand Name *</label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            className="w-full px-4 py-2.5 bg-gray-700 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
            placeholder="e.g. Hotel Three Leaves"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-300 mb-1">Official Website *</label>
          <input
            type="text"
            value={officialWebsite}
            onChange={(e) => setOfficialWebsite(e.target.value)}
            required
            className="w-full px-4 py-2.5 bg-gray-700 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
            placeholder="e.g. https://hotelthreeleaves.com"
          />
          <p className="text-xs text-gray-400 mt-1">Your legitimate corporate website URL</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-1">Industry</label>
            <input
              type="text"
              value={industry}
              onChange={(e) => setIndustry(e.target.value)}
              className="w-full px-4 py-2.5 bg-gray-700 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
              placeholder="e.g. Hospitality / Tourism"
            />
          </div>

          {isEditing && (
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Status</label>
              <select
                value={status}
                onChange={(e) => setStatus(e.target.value)}
                className="w-full px-4 py-2.5 bg-gray-700 border border-gray-600 rounded-lg text-white text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="ACTIVE">ACTIVE</option>
                <option value="PAUSED">PAUSED</option>
                <option value="ARCHIVED">ARCHIVED</option>
              </select>
            </div>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-300 mb-1">Description</label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={2}
            className="w-full px-4 py-2.5 bg-gray-700 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none text-sm"
            placeholder="Brief description of the brand"
          />
        </div>

        {!isEditing && (
          <>
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Keywords (comma-separated)</label>
              <input
                type="text"
                value={keywords}
                onChange={(e) => setKeywords(e.target.value)}
                className="w-full px-4 py-2.5 bg-gray-700 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
                placeholder="e.g. hotel, three leaves, suites"
              />
              <p className="text-xs text-gray-400 mt-1">Associated brand keywords to flag suspicious registrations</p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-1">Official Domains (comma-separated)</label>
              <input
                type="text"
                value={officialDomains}
                onChange={(e) => setOfficialDomains(e.target.value)}
                className="w-full px-4 py-2.5 bg-gray-700 border border-gray-600 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
                placeholder="e.g. hotelthreeleaves.com, hotelthreeleaves.in"
              />
              <p className="text-xs text-gray-400 mt-1">Domains owned by you (whitelisted from threat scans)</p>
            </div>
          </>
        )}

        <div className="flex gap-3 pt-3">
          <button
            type="submit"
            disabled={submitting}
            className="bg-blue-600 hover:bg-blue-700 disabled:bg-blue-800 text-white font-medium px-6 py-2.5 rounded-lg transition text-sm"
          >
            {submitting ? 'Saving...' : isEditing ? 'Update Brand' : 'Create Brand'}
          </button>
          <button
            type="button"
            onClick={() => navigate('/brands')}
            className="bg-gray-700 hover:bg-gray-600 text-white px-6 py-2.5 rounded-lg transition text-sm"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
