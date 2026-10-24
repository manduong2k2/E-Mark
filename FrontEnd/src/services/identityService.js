// services/identityService.js — Identity & Access (admin): users and roles
import { apiService } from './apiService';

const toQuery = (params = {}) => {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') query.append(key, value);
  });
  const text = query.toString();
  return text ? `?${text}` : '';
};

export const userAdminService = {
  // params: page, size, sortBy, sortOrder, search, roleId, status
  list: (params) => apiService.fetch(`/api/users${toQuery(params)}`),
  get: (id) => apiService.fetch(`/api/users/${id}`),
  // silent: forms show field errors (422) inline instead of a popup
  create: (data) => apiService.fetch('/api/users', { method: 'POST', body: data, silent: true }),
  update: (id, data) => apiService.fetch(`/api/users/${id}`, { method: 'PUT', body: data, silent: true }),
  remove: (id) => apiService.fetch(`/api/users/${id}`, { method: 'DELETE' }),
  grantRole: (roleId, userIds) =>
    apiService.fetch(`/api/users/role-grant/${roleId}`, { method: 'POST', body: { userIds } }),
  revokeRole: (roleId, userIds) =>
    apiService.fetch(`/api/users/role-revoke/${roleId}`, { method: 'POST', body: { userIds } }),
};

export const roleAdminService = {
  // params: page, size, sortBy, sortOrder, search
  list: (params) => apiService.fetch(`/api/roles${toQuery(params)}`),
  get: (id) => apiService.fetch(`/api/roles/${id}`),
  users: (id, params) => apiService.fetch(`/api/roles/${id}/users${toQuery(params)}`),
  create: (data) => apiService.fetch('/api/roles', { method: 'POST', body: data, silent: true }),
  update: (id, data) => apiService.fetch(`/api/roles/${id}`, { method: 'PUT', body: data, silent: true }),
  remove: (id) => apiService.fetch(`/api/roles/${id}`, { method: 'DELETE' }),
};

export const sessionAdminService = {
  // params: page, size, sortBy, sortOrder, search (IP), userId, createdFrom, createdTo
  list: (params) => apiService.fetch(`/api/admin/sessions${toQuery(params)}`),
  terminate: (id) => apiService.fetch(`/api/admin/sessions/${id}`, { method: 'DELETE' }),
  terminateMany: (sessionIds) =>
    apiService.fetch('/api/admin/sessions', { method: 'DELETE', body: { sessionIds } }),
};
