import React from 'react';

const EmptyState = () => {
  return (
    <div className="min-h-screen bg-[#E7F9FF] flex items-start justify-center p-6">
      <div
        className="bg-white rounded-2xl shadow-lg flex flex-col items-center justify-center gap-8 p-12"
        style={{
          marginTop: '24px',
          marginLeft: '40px',
          maxWidth: '1360px',
          minHeight: '852px',
          width: '100%',
        }}
      >
        {/* Empty State Icon/Illustration */}
        <div className="flex flex-col items-center justify-center gap-8">
          {/* Illustration Container */}
          <div className="relative">
            <div className="w-64 h-64 bg-gradient-to-br from-blue-100 to-blue-50 rounded-full flex items-center justify-center">
              <div className="w-48 h-48 bg-white rounded-full flex items-center justify-center shadow-inner">
                <svg
                  className="w-32 h-32 text-blue-400"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={1.5}
                    d="M3.055 11H5a2 2 0 012 2v1a2 2 0 002 2 2 2 0 012 2v2.945M8 3.935V5.5A2.5 2.5 0 0010.5 8h.5a2 2 0 012 2 2 2 0 104 0 2 2 0 012-2h1.064M15 20.488V18a2 2 0 012-2h3.064M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                  />
                </svg>
              </div>
            </div>
          </div>

          {/* Text Content */}
          <div className="flex flex-col items-center gap-4 max-w-md text-center">
            <h2 className="text-3xl font-bold text-gray-800">
              No Trips Planned Yet
            </h2>
            <p className="text-lg text-gray-500 leading-relaxed">
              Start planning your dream vacation! Explore our amazing destinations
              and create unforgettable memories.
            </p>
          </div>

          {/* Call to Action Button */}
          <div className="flex gap-4 mt-4">
            <button className="px-8 py-4 bg-blue-500 text-white text-lg font-semibold rounded-lg hover:bg-blue-600 transition-colors shadow-md hover:shadow-lg">
              Explore Destinations
            </button>
            <button className="px-8 py-4 bg-white text-blue-500 text-lg font-semibold rounded-lg border-2 border-blue-500 hover:bg-blue-50 transition-colors">
              View Packages
            </button>
          </div>
        </div>

        {/* Optional: Feature Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-12 w-full max-w-4xl">
          <div className="flex flex-col items-center gap-3 p-6 bg-blue-50 rounded-xl">
            <div className="w-12 h-12 bg-blue-500 rounded-full flex items-center justify-center">
              <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 01-9 9m9-9a9 9 0 00-9-9m9 9H3m9 9a9 9 0 01-9-9m9 9c1.657 0 3-4.03 3-9s-1.343-9-3-9m0 18c-1.657 0-3-4.03-3-9s1.343-9 3-9m-9 9a9 9 0 019-9" />
              </svg>
            </div>
            <h3 className="font-semibold text-gray-800">Worldwide Destinations</h3>
            <p className="text-sm text-gray-600 text-center">Choose from hundreds of amazing locations</p>
          </div>

          <div className="flex flex-col items-center gap-3 p-6 bg-blue-50 rounded-xl">
            <div className="w-12 h-12 bg-blue-500 rounded-full flex items-center justify-center">
              <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <h3 className="font-semibold text-gray-800">Best Prices</h3>
            <p className="text-sm text-gray-600 text-center">Affordable packages for every budget</p>
          </div>

          <div className="flex flex-col items-center gap-3 p-6 bg-blue-50 rounded-xl">
            <div className="w-12 h-12 bg-blue-500 rounded-full flex items-center justify-center">
              <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
              </svg>
            </div>
            <h3 className="font-semibold text-gray-800">Safe & Secure</h3>
            <p className="text-sm text-gray-600 text-center">Book with confidence and peace of mind</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default EmptyState;

