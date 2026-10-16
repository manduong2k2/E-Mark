// Sample orders for the vendor Orders page until a vendor orders API exists.
// Same shape as the backend OrderResponse / OrderItemResponse (Order context):
//   order: id, userId, status, total, name, phone, address, note, items, createdAt, updatedAt
//   item:  id, productId, productName, productSku, price, quantity, total, productImages, productDescription
// Replace SAMPLE_ORDERS with the API response; the page only depends on these fields.

const hoursAgo = (hours) => new Date(Date.now() - hours * 3600_000).toISOString();

const item = (id, productName, productSku, price, quantity) => ({
  id,
  productId: `0198f0a1-0000-7000-8000-${id.slice(-12)}`,
  productName,
  productSku,
  price,
  quantity,
  total: price * quantity,
  productImages: [],
  productDescription: null,
});

export const SAMPLE_ORDERS = [
  {
    id: '0198f2c4-1a2b-7c3d-9e4f-a1b2c3d4e5f6',
    userId: '0198e9aa-1111-7aaa-8bbb-000000000001',
    status: 'PENDING',
    name: 'Nguyễn Văn An',
    phone: '0901 234 567',
    address: '12 Nguyễn Trãi, Phường Bến Thành, Thành phố Hồ Chí Minh',
    note: 'Please call before delivering',
    items: [
      item('0198f2c4-aaaa-7000-8000-000000000101', 'Wireless Headphones X2', 'WH-X2-BLK', 79.9, 1),
      item('0198f2c4-aaaa-7000-8000-000000000102', 'USB-C Cable 2m', 'CAB-C2M', 9.5, 2),
    ],
    createdAt: hoursAgo(2),
    updatedAt: hoursAgo(2),
  },
  {
    id: '0198f1b7-2b3c-7d4e-8f50-b2c3d4e5f607',
    userId: '0198e9aa-1111-7aaa-8bbb-000000000002',
    status: 'PAID',
    name: 'Trần Thị Bình',
    phone: '0912 345 678',
    address: '45 Lê Lợi, Phường Hải Châu, Thành phố Đà Nẵng',
    note: null,
    items: [item('0198f1b7-bbbb-7000-8000-000000000201', 'Smart Watch S5', 'SW-S5-SLV', 149, 1)],
    createdAt: hoursAgo(9),
    updatedAt: hoursAgo(9),
  },
  {
    id: '0198eea0-3c4d-7e5f-9061-c3d4e5f60718',
    userId: '0198e9aa-1111-7aaa-8bbb-000000000003',
    status: 'PROCESSING',
    name: 'Lê Hoàng Cường',
    phone: '0987 654 321',
    address: '8 Tràng Tiền, Phường Hoàn Kiếm, Thành phố Hà Nội',
    note: 'Gift wrap if possible',
    items: [
      item('0198eea0-cccc-7000-8000-000000000301', 'Mechanical Keyboard K8', 'KB-K8-RGB', 89, 1),
      item('0198eea0-cccc-7000-8000-000000000302', 'Mouse Pad XL', 'MP-XL', 15, 1),
    ],
    createdAt: hoursAgo(30),
    updatedAt: hoursAgo(20),
  },
  {
    id: '0198e8f3-4d5e-7f60-a172-d4e5f6071829',
    userId: '0198e9aa-1111-7aaa-8bbb-000000000004',
    status: 'SHIPPING',
    name: 'Phạm Minh Đức',
    phone: '0933 112 233',
    address: '120 Trần Phú, Phường Lộc Thọ, Tỉnh Khánh Hòa',
    note: null,
    items: [item('0198e8f3-dddd-7000-8000-000000000401', 'Wireless Headphones X2', 'WH-X2-WHT', 79.9, 2)],
    createdAt: hoursAgo(72),
    updatedAt: hoursAgo(40),
  },
  {
    id: '0198e2d1-5e6f-7071-b283-e5f607182930',
    userId: '0198e9aa-1111-7aaa-8bbb-000000000005',
    status: 'COMPLETED',
    name: 'Võ Thu Hà',
    phone: '0977 888 999',
    address: '3 Hùng Vương, Phường Vĩnh Ninh, Thành phố Huế',
    note: null,
    items: [
      item('0198e2d1-eeee-7000-8000-000000000501', 'Smart Watch S5', 'SW-S5-BLK', 149, 1),
      item('0198e2d1-eeee-7000-8000-000000000502', 'Watch Strap Leather', 'WS-LTH', 25, 2),
    ],
    createdAt: hoursAgo(150),
    updatedAt: hoursAgo(100),
  },
  {
    id: '0198dc77-6f70-7182-c394-f60718293a41',
    userId: '0198e9aa-1111-7aaa-8bbb-000000000006',
    status: 'CANCELLED',
    name: 'Đặng Quốc Huy',
    phone: '0966 444 555',
    address: '77 Hai Bà Trưng, Phường Ninh Kiều, Thành phố Cần Thơ',
    note: 'Changed my mind',
    items: [item('0198dc77-ffff-7000-8000-000000000601', 'USB-C Cable 2m', 'CAB-C2M', 9.5, 3)],
    createdAt: hoursAgo(200),
    updatedAt: hoursAgo(190),
  },
];

/** Order total from its lines (the backend stores it as order.total). */
export const orderTotal = (order) => order.total ?? order.items.reduce((sum, line) => sum + line.total, 0);

/** Next step a vendor takes for each status (backend OrderStatusEnum lifecycle). */
export const NEXT_STATUS = {
  PENDING: { status: 'PROCESSING', label: 'Accept order', icon: 'bi-check2-circle' },
  PAID: { status: 'PROCESSING', label: 'Start processing', icon: 'bi-gear' },
  PROCESSING: { status: 'SHIPPING', label: 'Hand over to carrier', icon: 'bi-truck' },
  SHIPPING: { status: 'COMPLETED', label: 'Mark as delivered', icon: 'bi-box-seam' },
};

/** Statuses from which the vendor may still cancel (nothing has left the warehouse yet). */
export const CANCELLABLE = ['PENDING', 'PAID', 'PROCESSING'];

/** Orders the vendor still has to act on. */
export const TO_HANDLE = ['PENDING', 'PAID', 'PROCESSING'];
