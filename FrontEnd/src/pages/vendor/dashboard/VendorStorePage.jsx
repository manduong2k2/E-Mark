// Create (/my-vendor/store/new) or edit (/my-vendor/store) the current user's store.
import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { vendorService } from '../../../services/vendorService';
import { Field, PageHeader } from '../../admin/shared/AdminUi';
import { readErrors } from '../../admin/shared/adminUiUtils';
import { useVendor } from './vendorUtils';

const MAX_IMAGE_BYTES = 5 * 1024 * 1024;
const TAX_CODE = /^[A-Za-z0-9]{5,100}$/;
const PHONE = /^\+?[0-9]{7,15}$/;
const EMAIL = /^\S+@\S+\.\S+$/;

/** Image picker with preview: keeps the current image until a new file is chosen. */
function ImagePicker({ label, hint, current, file, onPick, error, wide, shape }) {
  const inputRef = useRef(null);
  const preview = useMemo(() => (file ? URL.createObjectURL(file) : null), [file]);
  useEffect(() => () => { if (preview) URL.revokeObjectURL(preview); }, [preview]);

  const shown = preview || current;

  return (
    <div className={`admin-ui-field${wide ? ' admin-ui-field--wide' : ''}${error ? ' has-error' : ''}`}>
      <span className="admin-ui-field-label">{label}</span>
      <button
        type="button"
        className={`vendor-image-picker vendor-image-picker--${shape}`}
        onClick={() => inputRef.current?.click()}
        aria-label={`Choose ${label.toLowerCase()}`}
      >
        {shown ? <img src={shown} alt="" /> : (
          <span className="vendor-image-placeholder">
            <i className="bi bi-image"></i>
            <span>Choose an image</span>
          </span>
        )}
        <span className="vendor-image-overlay"><i className="bi bi-camera"></i> {shown ? 'Change' : 'Upload'}</span>
      </button>
      <input
        ref={inputRef}
        type="file"
        accept="image/*"
        hidden
        onChange={(e) => { onPick(e.target.files?.[0] || null); e.target.value = ''; }}
      />
      {error ? <span className="admin-ui-field-error">{error}</span> : <span className="admin-ui-field-hint">{hint}</span>}
    </div>
  );
}

export default function VendorStorePage() {
  const { vendor, reloadVendor } = useVendor();
  const isCreate = !vendor;
  const navigate = useNavigate();

  const [form, setForm] = useState({
    name: vendor?.name || '',
    email: vendor?.email || '',
    phone: vendor?.phone || '',
    taxCode: vendor?.taxCode || '',
    description: vendor?.description || '',
  });
  const [logoFile, setLogoFile] = useState(null);
  const [bannerFile, setBannerFile] = useState(null);
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    document.title = isCreate ? 'My Store - Create store' : 'My Store - Store settings';
  }, [isCreate]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm(prev => ({ ...prev, [name]: value }));
    if (errors[name]) setErrors(prev => ({ ...prev, [name]: '' }));
  };

  const pickImage = (field, setter) => (file) => {
    if (file && !file.type.startsWith('image/')) {
      setErrors(prev => ({ ...prev, [field]: 'Please choose an image file' }));
      return;
    }
    if (file && file.size > MAX_IMAGE_BYTES) {
      setErrors(prev => ({ ...prev, [field]: 'The image must not exceed 5 MB' }));
      return;
    }
    setErrors(prev => ({ ...prev, [field]: '' }));
    setter(file);
  };

  // Same rules as RegisterVendorRequest, so most mistakes are caught before the round trip
  const validate = () => {
    const next = {};
    if (!form.name.trim()) next.name = 'Store name is required';
    else if (form.name.trim().length > 100) next.name = 'At most 100 characters';
    if (!TAX_CODE.test(form.taxCode.trim())) next.taxCode = '5 to 100 letters or digits';
    if (form.email.trim() && !EMAIL.test(form.email.trim())) next.email = 'Please enter a valid email';
    if (form.phone.trim() && !PHONE.test(form.phone.trim().replace(/[\s.-]/g, ''))) next.phone = '7 to 15 digits, optionally starting with +';
    if (form.description.length > 500) next.description = 'At most 500 characters';
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    const fd = new FormData();
    fd.append('name', form.name.trim());
    fd.append('taxCode', form.taxCode.trim());
    fd.append('email', form.email.trim());
    fd.append('phone', form.phone.trim().replace(/[\s.-]/g, ''));
    fd.append('description', form.description.trim());
    if (logoFile) fd.append('logo', logoFile);
    if (bannerFile) fd.append('banner', bannerFile);

    setSaving(true);
    try {
      const res = isCreate ? await vendorService.register(fd) : await vendorService.update(vendor.id, fd);
      if (res.ok) {
        window.showSuccess(isCreate ? 'Your store has been created. An admin will review it shortly.' : 'Store updated');
        await reloadVendor();
        navigate('/my-vendor');
      } else {
        const { fields, message } = readErrors(res);
        setErrors(fields);
        if (message) window.showError(message);
      }
    } catch {
      window.showError('Server connection error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="vendor-page">
      {isCreate && (
        <PageHeader
          eyebrow="Seller"
          eyebrowIcon="bi-shop"
          title="Create your store"
          description="Tell customers who you are. An admin reviews new stores before they can sell."
        />
      )}

      <form className="admin-ui-card vendor-form-card" onSubmit={handleSubmit} noValidate>
        <section className="vendor-form-section">
          <div className="vendor-form-section-head">
            <h2>Branding</h2>
            <p>Shown on your public store page and next to your products.</p>
          </div>
          <div className="admin-ui-form vendor-branding">
            <ImagePicker label="Logo" hint="Square image, at most 5 MB." shape="square"
              current={vendor?.logo} file={logoFile} onPick={pickImage('logo', setLogoFile)} error={errors.logo} />
            <ImagePicker label="Banner" hint="Wide image (about 3:1), at most 5 MB." shape="wide"
              current={vendor?.banner} file={bannerFile} onPick={pickImage('banner', setBannerFile)} error={errors.banner} />
          </div>
        </section>

        <section className="vendor-form-section">
          <div className="vendor-form-section-head">
            <h2>Store information</h2>
            <p>How customers and the marketplace reach you.</p>
          </div>
          <div className="admin-ui-form admin-ui-form--grid">
            <Field label="Store name *" error={errors.name} wide>
              <input name="name" value={form.name} onChange={handleChange} maxLength={100} placeholder="e.g. Saigon Gadgets" autoFocus={isCreate} />
            </Field>
            <Field label="Contact email" error={errors.email} hint="Customers use it to contact your store.">
              <input name="email" type="email" value={form.email} onChange={handleChange} maxLength={100} placeholder="store@example.com" />
            </Field>
            <Field label="Phone" error={errors.phone}>
              <input name="phone" type="tel" value={form.phone} onChange={handleChange} maxLength={20} placeholder="e.g. 0901234567" />
            </Field>
            <Field label="Tax code *" error={errors.taxCode} hint="Letters and digits only.">
              <input name="taxCode" value={form.taxCode} onChange={handleChange} maxLength={100} placeholder="e.g. 0312345678" />
            </Field>
            <label className={`admin-ui-field admin-ui-field--wide${errors.description ? ' has-error' : ''}`}>
              <span className="admin-ui-field-label">Description</span>
              <textarea name="description" value={form.description} onChange={handleChange} rows={4} maxLength={500}
                placeholder="What do you sell? What makes your store special?" />
              {errors.description
                ? <span className="admin-ui-field-error">{errors.description}</span>
                : <span className="admin-ui-field-hint">{form.description.length} / 500</span>}
            </label>
          </div>
        </section>

        <div className="vendor-form-footer">
          <Link to="/my-vendor" className="admin-ui-btn admin-ui-btn--ghost">Cancel</Link>
          <button type="submit" className="admin-ui-btn admin-ui-btn--primary" disabled={saving}>
            <i className="bi bi-check2"></i> {saving ? 'Saving…' : isCreate ? 'Create store' : 'Save changes'}
          </button>
        </div>
      </form>
    </div>
  );
}
