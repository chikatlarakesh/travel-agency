/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{js,jsx,ts,tsx}'],
  theme: {
    extend: {
      colors: {
        /* Figma Blue/Blue 03 — main page background */
        'blue-03': '#E7F9FF',
        primary: {
          DEFAULT: '#0077b6',
          dark: '#005f94',
          light: '#cce9f6',
        },
        secondary: '#00b4d8',
        accent: {
          DEFAULT: '#f77f00',
          dark: '#d46b00',
          light: '#fff3e0',
        },
        brand: {
          text: '#1a1a2e',
          muted: '#6c757d',
          bg: '#ffffff',
          'bg-light': '#f8f9fa',
        },
        /* cancellation */
        cancel: {
          yes: { bg: '#e8f7f0', text: '#15803d' },
          no:  { bg: '#fde8e8', text: '#b91c1c' },
        },
      },
      fontFamily: {
        nunito: ['Nunito', 'sans-serif'],
        poppins: ['Poppins', 'sans-serif'],
        sans: ['Poppins', 'Inter', 'Segoe UI', 'Tahoma', 'Verdana', 'sans-serif'],
      },
      fontSize: {
        h1: ['48px', { lineHeight: '48px', fontWeight: '500' }],
      },
      boxShadow: {
        soft:     '0 4px 24px rgba(15, 23, 42, 0.08)',
        card:     '0 2px 16px rgba(0, 119, 182, 0.08)',
        dropdown: '0 8px 30px rgba(15, 23, 42, 0.14)',
        filter:   '0 4px 20px rgba(0, 119, 182, 0.10)',
      },
      borderRadius: {
        pill: '9999px',
      },
    },
  },
  plugins: [],
};

