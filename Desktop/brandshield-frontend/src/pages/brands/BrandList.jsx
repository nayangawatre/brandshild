import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../../api/axios';
import { extractPageContent } from '../../utils/apiHelper';
import { HiPlus, HiShieldCheck } from 'react-icons/hi2';
import toast, { Toaster } from 'react-hot-toast';

export default function BrandList() {
  const [brands, setBrands] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get('/brands')
      .then((res) => setBrands(extractPageContent(res.data)))
      .catch(() => toast.error('Failed to load brands'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <Toaster position="top-right" />
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Brands</h1>
        <Link
          to="/brands/new"
          className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition"
        >
          <HiPlus /> Add Brand
        </Link>
      </div>

      {!brands || brands.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <HiShieldCheck className="text-5xl mx-auto mb-3 text-gray-600" />
          <p className="text-lg">No brands registered yet</p>
          <p className="text-sm mt-1">Add your first brand to start monitoring</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {brands.map((brand) => (
            <Link
              key={brand.id}
              to={`/brands/${brand.id}`}
              className="bg-gray-800 border border-gray-700 rounded-xl p-6 hover:border-blue-500/50 transition group"
            >
              <div className="flex items-start justify-between">
                <div>
                  <h3 className="text-lg font-semibold text-white group-hover:text-blue-400 transition">
                    {brand.name}
                  </h3>
                  {brand.officialWebsite && (
                    <p className="text-xs text-blue-400 mt-0.5">{brand.officialWebsite}</p>
                  )}
                  {brand.description && (
                    <p className="text-sm text-gray-400 mt-1 line-clamp-2">{brand.description}</p>
                  )}
                </div>
                <span
                  className={`px-2.5 py-1 rounded-full text-xs font-medium ${
                    brand.status === 'ACTIVE'
                      ? 'bg-green-900/50 text-green-400'
                      : brand.status === 'PAUSED'
                      ? 'bg-yellow-900/50 text-yellow-400'
                      : 'bg-gray-700 text-gray-400'
                  }`}
                >
                  {brand.status}
                </span>
              </div>

              <div className="mt-4 flex flex-wrap gap-2">
                {brand.keywords?.slice(0, 4).map((kw) => (
                  <span key={kw} className="bg-gray-700 text-gray-300 text-xs px-2 py-1 rounded">
                    {kw}
                  </span>
                ))}
                {brand.keywords?.length > 4 && (
                  <span className="text-xs text-gray-500">+{brand.keywords.length - 4} more</span>
                )}
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
