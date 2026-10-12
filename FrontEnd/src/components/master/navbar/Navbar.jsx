import { useContext, useState, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { AuthContext } from '../../../contexts/AuthContext';
import { CartContext } from '../../../contexts/CartContext';
import { authService } from '../../../services/authService';
import { useTheme } from '../../../hooks/useTheme';
import './Navbar.css';
import shopIcon from '../../../../public/logo.png';
import { APP_NAME, APP_SLOGAN } from '../../../configs/constants';

function CartIcon({ count }) {
  return (
    <Link to="/cart" className="navbar-cart-btn" title="Cart">
      <i className="fas fa-shopping-cart"></i>
      {count > 0 && <span className="cart-badge">{count > 99 ? '99+' : count}</span>}
    </Link>
  );
}

export default function Navbar() {
  const { user, setUser } = useContext(AuthContext);
  const { cart } = useContext(CartContext);
  const [searchQuery, setSearchQuery] = useState('');
  const navigate = useNavigate();
  const { theme, toggleTheme } = useTheme();

  const cartCount = cart?.totalItemCount || 0;
  const location = useLocation();

  const handleLogout = async () => {
    try {
      await authService.logout();
      await setUser(null);
      navigate('/auth?action=login');
    } catch (err) {
      alert('Logout failed!');
    }
  };

  const handleSearchKeyDown = (e) => {
    if (e.key === 'Enter' && searchQuery.trim()) {
      navigate(`/home?search=${encodeURIComponent(searchQuery.trim())}`);
    }
  };

  useEffect(() => {
    const searchParam = new URLSearchParams(location.search).get('search');
    if (searchParam) {
      setSearchQuery(searchParam);
    }
  }, [location.search]);

  return (
    <nav className="navbar">
      <div className="navbar-left">
        <Link to="/home" className="navbar-logo-link">
          <img src={shopIcon} alt="Logo" className="navbar-icon" />
          <span className="navbar-logo">
            <span className="navbar-logo-title">{APP_NAME}</span>
            <span className="navbar-logo-subtitle">{APP_SLOGAN}</span>
          </span>
        </Link>
      </div>

      {location.pathname === '/home' && (
        <div className="navbar-center">
          <i className="fa-solid fa-magnifying-glass"></i>
          <input
            type="text"
            placeholder="Search products..."
            className="search-input"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onKeyDown={handleSearchKeyDown}
          />
          {searchQuery && (
            <button className="navbar-search-clear" onClick={() => {
              setSearchQuery('');
              navigate('/home');
            }}>
              ✕
            </button>
          )}
        </div>
      )}

      <div className="navbar-right">
        <button
          type="button"
          className="navbar-theme-toggle"
          onClick={toggleTheme}
          aria-label={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
          title={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
        >
          <i className={`bi ${theme === 'dark' ? 'bi-sun' : 'bi-moon-stars'}`} aria-hidden="true"></i>
        </button>
        {user ? (
          <>
            <CartIcon count={cartCount} />
            <div className="user-dropdown">
              <button className="user-dropdown-toggle" type="button" aria-label="User menu">
                {user.avatar ? (
                  <img src={user.avatar} alt="Avatar" referrerPolicy="no-referrer" className="user-dropdown-avatar" />
                ) : (
                  <span className="user-dropdown-avatar user-dropdown-avatar--fallback">
                    {user.name?.charAt(0)?.toUpperCase() || '?'}
                  </span>
                )}
                <span className="user-dropdown-name">
                  {user.name}
                </span>
                <i className="fas fa-chevron-down user-dropdown-caret"></i>
              </button>

              <ul className="user-dropdown-menu">
                <li>
                  <div className="user-dropdown-header">
                    <span className="user-dropdown-header-name">{user.name}</span>
                    <span className="user-dropdown-header-email">{user.email}</span>
                  </div>
                </li>
                <li><hr className="user-dropdown-divider" /></li>
                <li>
                  <button className="user-dropdown-item" onClick={() => navigate('/profile')}>
                    <i className="fas fa-user"></i> Profile
                  </button>
                </li>
                <li>
                  <button className="user-dropdown-item" onClick={() => navigate('/orders')}>
                    <i className="fas fa-shopping-cart"></i> Order History
                  </button>
                </li>
                <li>
                  <button className="user-dropdown-item" onClick={() => navigate('/my-vendor')}>
                    <i className="fas fa-store"></i> Vendor Management
                  </button>
                </li>
                {user.roles.includes('Admin') && (
                  <>
                    <li><hr className="user-dropdown-divider" /></li>
                    <li>
                      <button className="user-dropdown-item user-dropdown-item--admin" onClick={() => navigate('/admin')}>
                        <i className="fas fa-cogs"></i> System Management
                      </button>
                    </li>
                  </>
                )}
                <li><hr className="user-dropdown-divider" /></li>
                <li>
                  <button className="user-dropdown-item user-dropdown-item--danger" onClick={handleLogout}>
                    <i className="fas fa-sign-out-alt"></i> Logout
                  </button>
                </li>
              </ul>
            </div>
          </>
        ) : (
          <>
            <Link to="/auth?action=login" className="navbar-btn">
              Login
            </Link>
            <Link to="/auth?action=register" className="navbar-btn">
              Register
            </Link>
          </>
        )}
      </div>
    </nav>
  );
}