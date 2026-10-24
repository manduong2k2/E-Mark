// Helpers shared by the vendor dashboard pages (kept apart from components for Fast Refresh)
import { useOutletContext } from 'react-router-dom';

/** { vendor, loading, reloadVendor } provided by VendorLayout to every /my-vendor page. */
export const useVendor = () => useOutletContext();

export const VENDOR_STATUS = {
  PENDING: { label: 'Pending approval', tone: 'warning' },
  ACTIVE: { label: 'Active', tone: 'success' },
  BANNED: { label: 'Banned', tone: 'danger' },
};

export const vendorStatus = (status) => VENDOR_STATUS[status] || { label: status || 'Unknown', tone: 'neutral' };

/** Products can only be added once an admin activated the store (activation grants the VENDOR role). */
export const canSell = (vendor) => vendor?.status === 'ACTIVE';

export const PRODUCT_STATUS = {
  PUBLISHED: { label: 'Published', tone: 'success' },
  DRAFT: { label: 'Draft', tone: 'neutral' },
  ARCHIVED: { label: 'Archived', tone: 'warning' },
};

export const LOW_STOCK = 10;

/** Variant images come either as plain URLs or as { url } objects. */
export const imageUrl = (img) => (typeof img === 'string' ? img : img?.url);
