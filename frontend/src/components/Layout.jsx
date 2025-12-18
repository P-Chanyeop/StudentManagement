import Sidebar from './Sidebar';
import Header from './Header';
import '../styles/Layout.css';

function Layout({ children }) {
  return (
    <div className="layout">
      <Sidebar />
      <div className="content-wrapper">
        <Header />
        <main className="main-content">
          {children}
        </main>
      </div>
    </div>
  );
}

export default Layout;
