import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import shopIcon from '../../../public/logo.png';
import { APP_NAME } from '../../configs/constants';
import { useTheme } from '../../hooks/useTheme';
import './OnboardingPage.css';

const ROLES = [
  {
    id: 'shopper',
    tag: { icon: 'fa-bag-shopping', label: 'Buyer Experience' },
    icon: 'fa-cart-shopping',
    title: 'I want to Shop',
    subtitle: 'Customer / Buyer Account',
    desc: 'Discover curated product catalog, exclusive buyer perks, streamlined checkout, and order tracking.',
    features: [
      { icon: 'fa-percent', text: 'Access exclusive welcome discount codes & coupon packs' },
      { icon: 'fa-shield-halved', text: 'Comprehensive buyer protection & verified merchant security' },
      { icon: 'fa-truck-fast', text: 'Real-time package tracking & flexible delivery options' },
    ],
    footer: 'Explore marketplace catalog',
    to: '/home',
  },
  {
    id: 'seller',
    tag: { icon: 'fa-chart-line', label: 'Merchant Portal' },
    icon: 'fa-store',
    title: 'I want to Sell',
    subtitle: 'Merchant / Business Account',
    desc: 'Launch your storefront, access analytics dashboards, inventory tools, and scale your audience.',
    features: [
      { icon: 'fa-rocket', text: 'Quick 3-step store registration with zero upfront setup fees' },
      { icon: 'fa-chart-pie', text: 'Powerful analytics dashboard & automated stock management' },
      { icon: 'fa-headset', text: 'Dedicated merchant support manager & growth toolkits' },
    ],
    footer: 'Configure your seller profile',
    to: '/my-vendor/store/new',
  },
];

const TRUST_ITEMS = [
  { icon: 'fa-repeat', title: 'Flexible Account Switch', desc: 'Toggle between buyer and seller mode anytime' },
  { icon: 'fa-shield', title: 'Encrypted Protection', desc: 'Enterprise data encryption & secured transactions' },
  { icon: 'fa-clock-rotate-left', title: '24/7 Support Service', desc: 'Dedicated assistance throughout your onboarding' },
];

export default function OnboardingPage() {
  const navigate = useNavigate();
  // Page has no navbar, so it owns the theme toggle
  const { theme, toggleTheme } = useTheme();
  const [selectedRole, setSelectedRole] = useState(null);

  useEffect(() => {
    document.title = `${APP_NAME} - Welcome`;
  }, []);

  const handleContinue = () => {
    const role = ROLES.find(r => r.id === selectedRole);
    if (role) navigate(role.to);
  };

  return (
    <div className="onboarding-page">
      {/* Background glow circles */}
      <div className="onboarding-glow onboarding-glow--top" />
      <div className="onboarding-glow onboarding-glow--bottom" />
      <div className="onboarding-glow onboarding-glow--center" />

      {/* Header */}
      <header className="onboarding-header">
        <div className="onboarding-header-inner">
          <div className="onboarding-brand">
            <div className="onboarding-brand-logo">
              <img src={shopIcon} alt="Logo" />
            </div>
            <span className="onboarding-brand-name">{APP_NAME}</span>
          </div>

          <button
            type="button"
            className="onboarding-theme-toggle"
            onClick={toggleTheme}
            aria-label={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
            title={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
          >
            <i className={`bi ${theme === 'dark' ? 'bi-sun' : 'bi-moon-stars'}`} aria-hidden="true"></i>
          </button>
        </div>
      </header>

      <main className="onboarding-main">
        {/* Hero */}
        <div className="onboarding-hero">
          <div className="onboarding-hero-badge">
            <i className="fa-solid fa-layer-group"></i>
            Tailor Your Marketplace Journey
          </div>
          <h1 className="onboarding-hero-title">How do you plan to use {APP_NAME}?</h1>
          <p className="onboarding-hero-desc">
            Choose your primary role. You can switch between buying and selling seamlessly at any time.
          </p>
        </div>

        {/* Role cards */}
        <div className="onboarding-roles">
          {ROLES.map(role => {
            const isSelected = selectedRole === role.id;
            return (
              <button
                key={role.id}
                type="button"
                className={`onboarding-role-card ${isSelected ? 'onboarding-role-card--selected' : ''}`}
                onClick={() => setSelectedRole(role.id)}
                aria-pressed={isSelected}
              >
                <span className="onboarding-check">
                  <i className="fa-solid fa-check"></i>
                </span>

                <div>
                  <span className="onboarding-role-tag">
                    <i className={`fa-solid ${role.tag.icon}`}></i> {role.tag.label}
                  </span>

                  <div className="onboarding-role-heading">
                    <div className="onboarding-role-icon">
                      <i className={`fa-solid ${role.icon}`}></i>
                    </div>
                    <div>
                      <h2 className="onboarding-role-title">{role.title}</h2>
                      <p className="onboarding-role-subtitle">{role.subtitle}</p>
                    </div>
                  </div>

                  <p className="onboarding-role-desc">{role.desc}</p>

                  <ul className="onboarding-role-features">
                    {role.features.map(f => (
                      <li key={f.text}>
                        <span className="onboarding-role-feature-icon">
                          <i className={`fa-solid ${f.icon}`}></i>
                        </span>
                        <span>{f.text}</span>
                      </li>
                    ))}
                  </ul>
                </div>

                <div className="onboarding-role-footer">
                  <span>{role.footer}</span>
                  <i className="fa-solid fa-arrow-right"></i>
                </div>
              </button>
            );
          })}
        </div>

        {/* Actions */}
        <div className="onboarding-actions">
          <button
            type="button"
            className="onboarding-continue-btn"
            disabled={!selectedRole}
            onClick={handleContinue}
          >
            <span>Continue</span>
            <i className="fa-solid fa-arrow-right"></i>
          </button>
        </div>

        {/* Trust footer */}
        <div className="onboarding-trust">
          {TRUST_ITEMS.map(item => (
            <div key={item.title} className="onboarding-trust-item">
              <i className={`fa-solid ${item.icon}`}></i>
              <div>
                <h4>{item.title}</h4>
                <p>{item.desc}</p>
              </div>
            </div>
          ))}
        </div>
      </main>
    </div>
  );
}
