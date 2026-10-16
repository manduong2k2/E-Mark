// Helpers shared by the store pages (kept out of Ui.jsx so it only exports components).
import { shortId } from '../../utils/ids';

/** Lower-case, accents removed, đ → d: "Đà Nẵng" → "da nang". */
export const normalizeText = (text = '') =>
  text.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/gi, 'd').toLowerCase().trim();

/** True when every word of the query appears in the text (accent-insensitive). */
export const matchesSearch = (text, query) => {
  const words = normalizeText(query).split(/\s+/).filter(Boolean);
  if (!words.length) return true;
  const haystack = normalizeText(text);
  return words.every(w => haystack.includes(w));
};

const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });
export const formatMoney = (value) => money.format(Number(value) || 0);

export const formatDate = (value, withTime = false) => {
  if (!value) return '—';
  const date = new Date(value);
  return withTime
    ? date.toLocaleString('en-GB', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })
    : date.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
};

/** Order statuses (backend OrderStatusEnum), shared with the admin and vendor pages. */
export { ORDER_STATUSES, orderStatus } from '../../utils/orderStatus';

/** Short, readable order number from its UUID. */
export const orderNumber = (id) => `#${shortId(id)}`;
