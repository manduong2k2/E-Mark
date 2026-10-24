// Order statuses = backend OrderStatusEnum (Order context). Used by the store, admin and vendor order pages.
// tone: a pill tone shared by both design systems (success | warning | danger | info | neutral).

export const ORDER_STATUSES = {
  PENDING: { label: 'Pending', tone: 'warning', icon: 'bi-hourglass-split' },
  PAID: { label: 'Paid', tone: 'info', icon: 'bi-credit-card' },
  PROCESSING: { label: 'Processing', tone: 'info', icon: 'bi-gear' },
  SHIPPING: { label: 'Shipping', tone: 'info', icon: 'bi-truck' },
  COMPLETED: { label: 'Completed', tone: 'success', icon: 'bi-check2-circle' },
  CANCELLED: { label: 'Cancelled', tone: 'danger', icon: 'bi-x-circle' },
  EXPIRED: { label: 'Expired', tone: 'neutral', icon: 'bi-clock-history' },
};

/** Label / tone / icon of a status; unknown values are shown as they come. */
export const orderStatus = (status) => {
  const key = (status || '').toUpperCase();
  return ORDER_STATUSES[key] || {
    label: key ? key.charAt(0) + key.slice(1).toLowerCase() : 'Unknown',
    tone: 'neutral',
    icon: 'bi-question-circle',
  };
};
