import { useState, useEffect, useContext, useRef } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { GoogleLogin } from '@react-oauth/google';
import { authService } from '../../services/authService';
import { loadFacebookSdk, facebookLogin } from '../../services/facebookSdk';
import { AuthContext } from '../../contexts/AuthContext';
import { showSuccess, showError } from '../../components/master/popup';
import './AuthPage.css';

export default function AuthPage() {
  useEffect(() => {
    document.title = 'My Store - Authentication';
  }, []);

  const [searchParams, setSearchParams] = useSearchParams();
  const action = searchParams.get('action') || 'login';
  const [isLogin, setIsLogin] = useState(action === 'login');
  const [rightPanelActive, setRightPanelActive] = useState(!isLogin);

  const [loginData, setLoginData] = useState({ email: '', password: '' });
  const [registerData, setRegisterData] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
    phone: ''
  });
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);

  const { setUser } = useContext(AuthContext);
  const navigate = useNavigate();

  // Preload the Facebook SDK so the login popup can open synchronously on click
  const facebookSdk = useRef(null);
  useEffect(() => {
    loadFacebookSdk()
      .then((FB) => { facebookSdk.current = FB; })
      .catch(() => { /* retried on click; the error is shown there */ });
  }, []);

  useEffect(() => {
    setIsLogin(action === 'login');
    setRightPanelActive(action !== 'login');
  }, [action]);

  const switchToSignUp = () => {
    setRightPanelActive(true);
    setIsLogin(false);
    setSearchParams({ action: 'register' }, { replace: true });
  };

  const switchToLogin = () => {
    setRightPanelActive(false);
    setIsLogin(true);
    setSearchParams({ action: 'login' }, { replace: true });
  };

  const togglePassword = (inputId, iconId) => {
    const input = document.getElementById(inputId);
    const icon = document.getElementById(iconId);
    if (input.type === "password") {
      input.type = "text";
      icon.classList.remove("fa-eye");
      icon.classList.add("fa-eye-slash");
    } else {
      input.type = "password";
      icon.classList.remove("fa-eye-slash");
      icon.classList.add("fa-eye");
    }
  };

  const handleLoginChange = (e) => {
    const { name, value } = e.target;
    setLoginData(prev => ({ ...prev, [name]: value }));
  };

  const handleRegisterChange = (e) => {
    const { name, value } = e.target;
    setRegisterData(prev => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const validateRegisterForm = () => {
    const newErrors = {};

    if (!registerData.name.trim()) {
      newErrors.name = 'Please enter name';
    }

    if (!registerData.email.trim()) {
      newErrors.email = 'Please enter email';
    } else if (!/\S+@\S+\.\S+/.test(registerData.email)) {
      newErrors.email = 'Invalid email format';
    }

    if (!registerData.password) {
      newErrors.password = 'Please enter password';
    } else if (registerData.password.length < 6) {
      newErrors.password = 'Password must be at least 6 characters';
    }

    if (!registerData.confirmPassword) {
      newErrors.confirmPassword = 'Please confirm password';
    } else if (registerData.password !== registerData.confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match';
    }

    if (registerData.phone && !/^\d{10,11}$/.test(registerData.phone.replace(/\s/g, ''))) {
      newErrors.phone = 'Invalid phone number';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleLogin = async (e) => {
    e.preventDefault();

    if (!loginData.email || !loginData.password) {
      showError('Please complete all required fields!', 'Login Error');
      return;
    }

    setLoading(true);
    try {
      const response = await authService.login({
        email: loginData.email,
        password: loginData.password
      });

      if (response.ok) {
        const user = await authService.profile();
        setUser(user.data.data);
        showSuccess(response.data.message, 'Welcome back');
        setTimeout(() => {
          navigate('/home');
        }, 1000);
      } else {
        showError(response.data.message || 'Login failed', 'Login Error');
      }
    } catch (err) {
      showError('Login failed! Please try again.', 'Connection Error');
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (e) => {
    e.preventDefault();

    if (!validateRegisterForm()) {
      return;
    }

    setLoading(true);
    try {
      const { confirmPassword, ...registerDataToSend } = registerData;
      const formDataToSend = new FormData();
      Object.keys(registerDataToSend).forEach(key => {
        formDataToSend.append(key, registerDataToSend[key]);
      });

      const response = await authService.register(formDataToSend);

      if (response.ok) {
        showSuccess('Registration successful!', 'Welcome to our platform');
        setTimeout(() => {
          switchToLogin();
        }, 1200);
      } else if (response.data && response.data.errors && typeof response.data.errors === 'object') {
        const backendErrors = {};
        Object.keys(response.data.errors).forEach(field => {
          backendErrors[field] = response.data.errors[field];
        });
        setErrors(backendErrors);
      } else {
        showError('Registration failed!' + (response.data.message ? ' - ' + response.data.message : ''), 'Please try again');
      }
    } catch (err) {
      showError('Registration failed!', err.message || 'Please try again');
    } finally {
      setLoading(false);
    }
  };

  // Sends the provider credential to the backend, which verifies it and signs the user in
  // (creating or linking the account by email when needed)
  const loginWithProvider = async (provider, label, credential) => {
    setLoading(true);
    try {
      const response = await authService.oauthLogin(provider, credential);

      if (response.ok) {
        const user = await authService.profile();
        setUser(user.data.data);
        showSuccess(response.data.message, 'Welcome');
        if(response.data.isNewUser) {
          setTimeout(() => {
            navigate('/onboarding');
          }, 1000);
        } else {
          setTimeout(() => {
            navigate('/home');
          }, 1000);
        }
      } else {
        showError(response.data.message || `${label} login failed`, `${label} Login`);
      }
    } catch {
      showError(`${label} login failed! Please try again.`, 'Connection Error');
    } finally {
      setLoading(false);
    }
  };

  // Google Sign-In returns an ID token (JWT) in response.credential
  const handleGoogleSuccess = (credentialResponse) => {
    const idToken = credentialResponse?.credential;
    if (!idToken) {
      showError('Google did not return a credential', 'Google Login');
      return;
    }
    loginWithProvider('google', 'Google', idToken);
  };

  // Facebook Login returns a user access token
  const handleFacebookLogin = async () => {
    try {
      // The SDK is preloaded on mount, so FB.login normally runs within the click and the popup is not blocked
      const FB = facebookSdk.current ?? await loadFacebookSdk();
      const accessToken = await facebookLogin(FB);
      if (!accessToken) {
        showError('Facebook sign-in was cancelled', 'Facebook Login');
        return;
      }
      loginWithProvider('facebook', 'Facebook', accessToken);
    } catch {
      showError('Facebook sign-in is unavailable right now. Please try again later.', 'Facebook Login');
    }
  };

  // "or" divider + Google/Facebook buttons, shared by the Login and Register tabs.
  // Google sign-in also creates the account when it does not exist yet, so it works for registration too.
  const renderSocialLogin = () => (
    <>
      <div className="auth-divider">
        <div className="auth-divider-line"></div>
        <span className="auth-divider-text">or</span>
      </div>

      <div className="auth-social-buttons">
        {/* Google's official button: returns the ID token in response.credential */}
        <div className="auth-google-btn">
          {/* shape="circle" only applies to type="icon" (standard buttons render it as a pill) */}
          <GoogleLogin
            onSuccess={handleGoogleSuccess}
            onError={() => showError('Google sign-in was cancelled or failed', 'Google Login')}
            type="icon"
            theme="outline"
            shape="circle"
            size="large"
          />
        </div>
        {/* Styled to match Google's round icon button */}
        <button
          type="button"
          onClick={handleFacebookLogin}
          disabled={loading}
          className="auth-facebook-btn"
          aria-label="Sign in with Facebook"
          title="Sign in with Facebook"
        >
          <i className="fa-brands fa-facebook-f auth-facebook-icon"></i>
        </button>
      </div>
    </>
  );

  return (
    <div className="auth-page-container">
      <div className={`auth-container ${rightPanelActive ? 'auth-right-panel-active' : ''}`} id="container">

        {/* Sign In Form Panel */}
        <div className="auth-form-container auth-sign-in-container">
          {/* Mobile only: arrow to switch to Sign Up */}
          <button type="button" onClick={switchToSignUp} className="auth-mobile-switch-btn auth-next">
            <span>Register</span>
            <i className="fa-solid fa-arrow-right"></i>
          </button>
          <form id="signInForm" onSubmit={handleLogin} className="auth-form">
            <div className="auth-form-header">
              <div className="auth-icon-wrapper">
                <i className="fa-solid fa-bolt"></i>
              </div>
              <h2>Welcome Back!</h2>
              <p>Please enter your credentials to access your account</p>
            </div>
            <div className="auth-form-content">


              <div className="auth-input-fields">
                <div className="auth-input-group">
                  <label>Email or Username</label>
                  <span className="auth-input-icon">
                    <i className="fa-regular fa-envelope"></i>
                  </span>
                  <input
                    type="text"
                    name="email"
                    value={loginData.email}
                    onChange={handleLoginChange}
                    placeholder="name@company.com"
                    required
                  />
                </div>

                <div className="auth-input-group">
                  <div className="auth-label-row">
                    <label>Password</label>
                  </div>
                  <span className="auth-input-icon">
                    <i className="fa-solid fa-lock"></i>
                  </span>
                  <input
                    type="password"
                    id="login-password"
                    name="password"
                    value={loginData.password}
                    onChange={handleLoginChange}
                    placeholder="••••••••"
                    required
                  />
                  <button type="button" onClick={() => togglePassword('login-password', 'login-eye')} className="auth-toggle-password">
                    <i id="login-eye" className="fa-regular fa-eye"></i>
                  </button>
                </div>

                <span className='d-flex justify-content-between align-items-center'>
                  <div className="auth-checkbox-group">
                    <input type="checkbox" id="remember" />
                    <label htmlFor="remember">Remember me</label>
                  </div>
                  <Link to="/forgot" className="auth-forgot-link">Forgot password?</Link>
                </span>
              </div>

              {renderSocialLogin()}

            </div>

            <div className="auth-submit-section">
              <button type="submit" className="auth-submit-btn" disabled={loading}>
                {loading ? 'Logging In...' : (
                  <>
                    <span>Login</span>
                    <i className="fa-solid fa-arrow-right"></i>
                  </>
                )}
              </button>
            </div>
          </form>
        </div>

        {/* Sign Up Form Panel */}
        <div className="auth-form-container auth-sign-up-container">
          {/* Mobile only: arrow to switch back to Sign In */}
          <button type="button" onClick={switchToLogin} className="auth-mobile-switch-btn auth-prev">
            <i className="fa-solid fa-arrow-left"></i>
            <span>Login</span>
          </button>
          <form id="signUpForm" onSubmit={handleRegister} className="auth-form">
            <div className="auth-form-content">
              <div className="auth-form-header">
                <div className="auth-icon-wrapper auth-purple">
                  <i className="fa-solid fa-user-plus"></i>
                </div>
                <h2>Create Account</h2>
                <p>Join us in just a few simple steps</p>
              </div>

              <div className="auth-input-fields">
                <div className="auth-input-group">
                  <label>Full Name</label>
                  <span className="auth-input-icon">
                    <i className="fa-regular fa-user"></i>
                  </span>
                  <input
                    type="text"
                    name="name"
                    value={registerData.name}
                    onChange={handleRegisterChange}
                    placeholder="John Doe"
                    required
                    className={errors.name ? 'auth-error' : ''}
                  />
                  {errors.name && <span className="auth-error-message">{errors.name}</span>}
                </div>

                <div className="auth-input-group">
                  <label>Email Address</label>
                  <span className="auth-input-icon">
                    <i className="fa-regular fa-envelope"></i>
                  </span>
                  <input
                    type="email"
                    name="email"
                    value={registerData.email}
                    onChange={handleRegisterChange}
                    placeholder="name@company.com"
                    required
                    className={errors.email ? 'auth-error' : ''}
                  />
                  {errors.email && <span className="auth-error-message">{errors.email}</span>}
                </div>

                <div className="auth-input-group">
                  <label>Password</label>
                  <span className="auth-input-icon">
                    <i className="fa-solid fa-lock"></i>
                  </span>
                  <input
                    type="password"
                    id="register-password"
                    name="password"
                    value={registerData.password}
                    onChange={handleRegisterChange}
                    placeholder="At least 6 characters"
                    required
                    className={errors.password ? 'auth-error' : ''}
                  />
                  <button type="button" onClick={() => togglePassword('register-password', 'reg-eye-1')} className="auth-toggle-password">
                    <i id="reg-eye-1" className="fa-regular fa-eye"></i>
                  </button>
                  {errors.password && <span className="auth-error-message">{errors.password}</span>}
                </div>

                <div className="auth-input-group">
                  <label>Confirm Password</label>
                  <span className="auth-input-icon">
                    <i className="fa-solid fa-shield-halved"></i>
                  </span>
                  <input
                    type="password"
                    id="register-confirm-password"
                    name="confirmPassword"
                    value={registerData.confirmPassword}
                    onChange={handleRegisterChange}
                    placeholder="Re-enter your password"
                    required
                    className={errors.confirmPassword ? 'auth-error' : ''}
                  />
                  <button type="button" onClick={() => togglePassword('register-confirm-password', 'reg-eye-2')} className="auth-toggle-password">
                    <i id="reg-eye-2" className="fa-regular fa-eye"></i>
                  </button>
                  {errors.confirmPassword && <span className="auth-error-message">{errors.confirmPassword}</span>}
                </div>

                <div className="auth-input-group">
                  <label>Phone (optional)</label>
                  <span className="auth-input-icon">
                    <i className="fa-solid fa-phone"></i>
                  </span>
                  <input
                    type="tel"
                    name="phone"
                    value={registerData.phone}
                    onChange={handleRegisterChange}
                    placeholder="Phone number"
                    className={errors.phone ? 'auth-error' : ''}
                  />
                  {errors.phone && <span className="auth-error-message">{errors.phone}</span>}
                </div>

                <div className="auth-checkbox-group">
                  <input type="checkbox" id="terms" required />
                  <label htmlFor="terms">
                    I agree to the <a href="#">Terms of Service</a> and <a href="#">Privacy Policy</a>.
                  </label>
                </div>
              </div>

              {renderSocialLogin()}
            </div>

            <div className="auth-submit-section">
              <button type="submit" className="auth-submit-btn auth-purple" disabled={loading}>
                {loading ? 'Registering...' : (
                  <>
                    <span>Register Now</span>
                    <i className="fa-solid fa-user-check"></i>
                  </>
                )}
              </button>
            </div>
          </form>
        </div>

        {/* Overlay Container */}
        <div className="auth-overlay-container">
          <div className="auth-overlay">
            <div className="auth-bg-shape auth-shape-1"></div>
            <div className="auth-bg-shape auth-shape-2"></div>

            <div className="auth-overlay-panel auth-overlay-left">
              <div className="auth-overlay-icon">
                <i className="fa-solid fa-sparkles"></i>
              </div>
              <h1>Already Have an Account?</h1>
              <p>Log in now to seamlessly continue your journey and access all features.</p>
              <button id="signInBtn" onClick={switchToLogin} className="auth-overlay-btn">
                Login
              </button>
            </div>

            <div className="auth-overlay-panel auth-overlay-right">
              <div className="auth-overlay-icon">
                <i className="fa-solid fa-rocket"></i>
              </div>
              <h1>Don't Have an Account?</h1>
              <p>Register in seconds to start your amazing experience with us today!</p>
              <button id="signUpBtn" onClick={switchToSignUp} className="auth-overlay-btn">
                Register
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}