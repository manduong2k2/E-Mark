// services/vendorService.js
import { request } from './apiService';

const toQuery = (params) => (params ? `?${new URLSearchParams(params)}` : '');

export const vendorService = {
  getAll: (params) => request(`/api/vendors${toQuery(params)}`),

  getById: (id) => request(`/api/vendors/${id}`),

  /** The current user's vendor; data is null when they have none */
  getMyVendor: () => request('/api/vendors/me'),

  /** Current user registers their store (multipart: name, description, logo, banner, taxCode, email, phone, addressId) */
  register: (formData) => request('/api/vendors/me', { method: 'POST', body: formData }),

  /** Owner or admin; multipart, same fields as register. Logo / banner are kept when no new file is sent */
  update: (id, formData) => request(`/api/vendors/${id}`, { method: 'PUT', body: formData }),

  /** Admin: activates a pending vendor (its owner then gets the VENDOR role) */
  approve: (id) => request(`/api/vendors/${id}/activate`, { method: 'POST' }),

  delete: (id) => request(`/api/vendors/${id}`, { method: 'DELETE' }),
};
