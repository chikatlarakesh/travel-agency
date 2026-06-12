import Navbar from '../components/Navbar/Navbar';

const MainLayout = ({ children }) => {
  return (
    /* Figma: background #E7F9FF (Blue/Blue 03) */
    <div className="flex min-h-screen flex-col bg-blue-03">
      <Navbar />
      <main className="flex-1">{children}</main>
    </div>
  );
};

export default MainLayout;
