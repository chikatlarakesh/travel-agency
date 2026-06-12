import React from 'react';

const EmptyStateSimple = () => {
  return (
    <div
      className="w-full min-h-screen flex items-center justify-center p-6"
      style={{ backgroundColor: '#E7F9FF' }}
    >
      <div
        className="bg-white rounded-3xl shadow-2xl p-16 flex flex-col items-center justify-center"
        style={{
          marginTop: '24px',
          marginLeft: '40px',
          width: '100%',
          maxWidth: '1360px',
          minHeight: '852px',
        }}
      >
        {/* Main Content Container */}
        <div className="flex flex-col items-center gap-8 max-w-2xl mx-auto">

          {/* Icon/Illustration */}
          <div className="mb-8">
            <svg
              className="w-48 h-48 text-blue-400"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1}
                d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"
              />
            </svg>
          </div>

          {/* Heading */}
          <h1 className="text-5xl font-bold text-gray-800 text-center">
            Start Your Journey
          </h1>

          {/* Description */}
          <p className="text-xl text-gray-600 text-center leading-relaxed max-w-lg">
            Your travel adventure begins here. Discover amazing destinations,
            create memorable experiences, and explore the world with us.
          </p>

          {/* Action Button */}
          <button className="mt-8 px-12 py-5 bg-gradient-to-r from-blue-500 to-blue-600 text-white text-xl font-bold rounded-full hover:from-blue-600 hover:to-blue-700 transition-all shadow-xl hover:shadow-2xl transform hover:scale-105">
            Get Started
          </button>

          {/* Stats or Features */}
          <div className="grid grid-cols-3 gap-12 mt-16 w-full">
            <div className="text-center">
              <div className="text-4xl font-bold text-blue-600">200+</div>
              <div className="text-sm text-gray-600 mt-2">Destinations</div>
            </div>
            <div className="text-center">
              <div className="text-4xl font-bold text-blue-600">50K+</div>
              <div className="text-sm text-gray-600 mt-2">Happy Travelers</div>
            </div>
            <div className="text-center">
              <div className="text-4xl font-bold text-blue-600">4.9★</div>
              <div className="text-sm text-gray-600 mt-2">Rating</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default EmptyStateSimple;

