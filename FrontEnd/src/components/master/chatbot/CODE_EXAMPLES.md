# Chatbot Component - Code Examples

## Backend Response Examples

### Example 1: Single Product
```json
{
  "success": true,
  "data": {
    "reply": "Here's the iPhone 15 Pro Max you were asking about!",
    "attachments": {
      "intent": "product",
      "data": {
        "id": "prod-001",
        "name": "iPhone 15 Pro Max",
        "image": "https://example.com/iphone15-pro.jpg",
        "price": 29999000,
        "category": "Smartphones",
        "rating": 4.8
      }
    },
    "model": "gemini-3.5-flash-lite"
  }
}
```

**Result in Chat**: Text reply + ProductCard component showing phone details

---

### Example 2: Product List (Trending Items)
```json
{
  "success": true,
  "data": {
    "reply": "Here are the trending products this week",
    "attachments": {
      "intent": "products",
      "data": {
        "currentPage": 0,
        "data": [
          {
            "id": "prod-001",
            "name": "iPhone 15 Pro",
            "image": "https://...",
            "price": 24999000,
            "category": "Smartphones",
            "rating": 4.8
          },
          {
            "id": "prod-002",
            "name": "Samsung Galaxy S24",
            "image": "https://...",
            "price": 22999000,
            "category": "Smartphones",
            "rating": 4.7
          },
          {
            "id": "prod-003",
            "name": "Google Pixel 8",
            "image": "https://...",
            "price": 18999000,
            "category": "Smartphones",
            "rating": 4.6
          }
        ],
        "hasNext": true,
        "hasPrevious": false,
        "pageSize": 3,
        "totalElements": 45,
        "totalPages": 15
      }
    },
    "model": "gemini-3.5-flash-lite"
  }
}
```

**Result in Chat**: Text reply + ProductList component showing 3 products with pagination info "Trang 1/15"

---

### Example 3: Single Category
```json
{
  "success": true,
  "data": {
    "reply": "Let me show you the Women Fashion category",
    "attachments": {
      "intent": "category",
      "data": {
        "id": "cat-001",
        "name": "Women Fashion",
        "image": "https://example.com/women-fashion.jpg",
        "description": "Latest women's fashion collection including dresses, tops, and accessories",
        "children": [
          {
            "id": "cat-002",
            "name": "Dresses",
            "parentId": "cat-001"
          },
          {
            "id": "cat-003",
            "name": "Tops",
            "parentId": "cat-001"
          },
          {
            "id": "cat-004",
            "name": "Accessories",
            "parentId": "cat-001"
          }
        ],
        "parentId": null
      }
    }
  }
}
```

**Result in Chat**: Text reply + CategoryCard showing "Women Fashion" with "3 danh mục con"

---

### Example 4: Category List
```json
{
  "success": true,
  "data": {
    "reply": "Here are the top categories in the system",
    "attachments": {
      "intent": "categories",
      "data": {
        "currentPage": 0,
        "data": [
          {
            "id": "cat-001",
            "name": "Women Fashion",
            "image": "https://...",
            "description": "Women's fashion items",
            "children": [
              {"id": "sub-1", "name": "Dresses"},
              {"id": "sub-2", "name": "Tops"}
            ]
          },
          {
            "id": "cat-002",
            "name": "Men Fashion",
            "image": "https://...",
            "description": "Men's fashion items",
            "children": [
              {"id": "sub-3", "name": "Shirts"},
              {"id": "sub-4", "name": "Pants"}
            ]
          },
          {
            "id": "cat-003",
            "name": "Fashion",
            "image": "https://...",
            "description": "All fashion items",
            "children": [
              {"id": "cat-001", "name": "Women Fashion"},
              {"id": "cat-002", "name": "Men Fashion"}
            ]
          }
        ],
        "hasNext": true,
        "hasPrevious": false,
        "pageSize": 3,
        "totalElements": 6,
        "totalPages": 2
      }
    }
  }
}
```

**Result in Chat**: Text reply + CategoryList showing 3 categories

---

### Example 5: Single Brand
```json
{
  "success": true,
  "data": {
    "reply": "Here's information about Nike, one of our top brands",
    "attachments": {
      "intent": "brand",
      "data": {
        "id": "brand-001",
        "name": "Nike",
        "logo": "https://example.com/nike-logo.png",
        "description": "Leading global athletic brand",
        "country": "USA"
      }
    }
  }
}
```

**Result in Chat**: Text reply + BrandCard showing Nike with logo and country

---

### Example 6: Brand List
```json
{
  "success": true,
  "data": {
    "reply": "Here are the premium brands we carry",
    "attachments": {
      "intent": "brands",
      "data": {
        "currentPage": 0,
        "data": [
          {
            "id": "brand-001",
            "name": "Nike",
            "logo": "https://...",
            "description": "Athletic brand",
            "country": "USA"
          },
          {
            "id": "brand-002",
            "name": "Adidas",
            "logo": "https://...",
            "description": "Sports brand",
            "country": "Germany"
          },
          {
            "id": "brand-003",
            "name": "Puma",
            "logo": "https://...",
            "description": "Sports lifestyle",
            "country": "Germany"
          }
        ],
        "hasNext": false,
        "hasPrevious": false,
        "pageSize": 10,
        "totalElements": 3,
        "totalPages": 1
      }
    }
  }
}
```

**Result in Chat**: Text reply + BrandList showing 3 brands

---

### Example 7: Single Vendor
```json
{
  "success": true,
  "data": {
    "reply": "This vendor has excellent customer reviews",
    "attachments": {
      "intent": "vendor",
      "data": {
        "id": "vendor-001",
        "name": "TechStore 24 - Premium Electronics",
        "logo": "https://example.com/techstore-logo.jpg",
        "description": "Specialized in latest electronics and gadgets",
        "rating": 4.9
      }
    }
  }
}
```

**Result in Chat**: Text reply + VendorCard showing shop info and rating

---

### Example 8: Vendor List
```json
{
  "success": true,
  "data": {
    "reply": "Here are the top-rated vendors selling electronics",
    "attachments": {
      "intent": "vendors",
      "data": {
        "currentPage": 0,
        "data": [
          {
            "id": "vendor-001",
            "name": "TechStore 24",
            "logo": "https://...",
            "description": "Premium electronics",
            "rating": 4.9
          },
          {
            "id": "vendor-002",
            "name": "GadgetWorld",
            "logo": "https://...",
            "description": "Latest gadgets",
            "rating": 4.8
          },
          {
            "id": "vendor-003",
            "name": "ElectroPlus",
            "logo": "https://...",
            "description": "Electronics superstore",
            "rating": 4.7
          }
        ],
        "hasNext": true,
        "hasPrevious": false,
        "pageSize": 5,
        "totalElements": 12,
        "totalPages": 3
      }
    }
  }
}
```

**Result in Chat**: Text reply + VendorList showing 3 vendors

---

### Example 9: Text Only (No Attachment)
```json
{
  "success": true,
  "data": {
    "reply": "I'd be happy to help you track your order. Could you provide your order number?",
    "model": "gemini-3.5-flash-lite"
  }
}
```

**Result in Chat**: Only text reply, no component rendering

---

### Example 10: Error Response
```json
{
  "success": false,
  "status": 401,
  "data": {
    "message": "Please log in to chat with the assistant"
  }
}
```

**Result in Chat**: Error message "Vui lòng đăng nhập để trò chuyện với Trợ lý AI."

---

## Frontend Code Examples

### Using the Chatbot Component

```javascript
// In your layout (e.g., MasterLayout.jsx or AdminLayout.jsx)
import Chatbot from '../../components/master/chatbot';

export default function MasterLayout() {
  return (
    <>
      <header>{/* ... */}</header>
      <main>{/* ... */}</main>
      <Chatbot />  {/* Auto-renders at bottom-right */}
    </>
  );
}
```

### API Call with Attachments

```javascript
// In your assistantService
async function chat(message, provider = 'gemini') {
  const response = await fetch('/api/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message, provider })
  });
  
  return {
    ok: response.ok,
    status: response.status,
    data: await response.json()
  };
}
```

### Chatbot Message Handling

```javascript
// In Chatbot.jsx
const handleSendMessage = async (text) => {
  // Add user message
  addMessage('user', text);
  
  // Get response
  const response = await assistantService.chat(text, provider);
  
  if (response.ok) {
    const { reply, attachments } = response.data?.data;
    
    // Add text reply
    addMessage('bot', reply);
    
    // Add component if attachments exist
    if (attachments?.intent && attachments?.data) {
      addMessage('bot', '', {
        isAttachment: true,
        attachmentIntent: attachments.intent,
        attachmentData: attachments.data
      });
    }
  }
};
```

### Direct Component Usage (Advanced)

```javascript
// Use AttachmentRenderer directly
import AttachmentRenderer from './sub-components/AttachmentRenderer';

export function MyComponent() {
  const attachmentData = {
    intent: 'products',
    data: {
      currentPage: 0,
      data: [/* ... */],
      totalPages: 1,
      // ...
    }
  };
  
  return (
    <div>
      <AttachmentRenderer 
        intent={attachmentData.intent}
        data={attachmentData.data}
      />
    </div>
  );
}
```

---

## Common Response Patterns

### Pattern: Search Results
```javascript
{
  reply: "Found 23 products matching 'iPhone'",
  attachments: {
    intent: "products",
    data: {
      currentPage: 0,
      data: [/* first 5 products */],
      totalPages: 5,
      // ...
    }
  }
}
```

### Pattern: Product Recommendation
```javascript
{
  reply: "Based on your preferences, I recommend this product:",
  attachments: {
    intent: "product",
    data: {
      id: "...",
      name: "...",
      // single product data
    }
  }
}
```

### Pattern: Navigation
```javascript
{
  reply: "Let me show you our main categories:",
  attachments: {
    intent: "categories",
    data: {
      currentPage: 0,
      data: [/* top 3 categories */],
      totalPages: 2,
      // ...
    }
  }
}
```

---

## Testing User Messages & Expected Responses

```javascript
// User: "Show me trending products"
// Expected: ProductList with trending items

// User: "Tell me about the iPhone 15"
// Expected: ProductCard with iPhone details

// User: "What categories do you have?"
// Expected: CategoryList

// User: "Show me brands"
// Expected: BrandList

// User: "Who are your sellers?"
// Expected: VendorList

// User: "How do I return an item?"
// Expected: Text-only reply with policy
```

---

## Troubleshooting

### Issue: Component Not Rendering

**Check 1**: Verify intent value
```javascript
// ✅ Works: "product", "products", "category", "categories"
// ❌ Fails: "Product", "CATEGORY", "items"
// Note: Case-insensitive but must match predefined values
```

**Check 2**: Verify data structure
```javascript
// For List - must have data.data as array
{
  data: [
    { id: "1", name: "..." },  // ✅ Correct
    { id: "2", name: "..." }
  ]
}

// ❌ Wrong - missing nested 'data' key
{
  items: [
    { id: "1", name: "..." }
  ]
}
```

**Check 3**: Check browser console
```javascript
// Should see warning if intent doesn't match
console.warn(`Unknown intent or no component for: unknown_type`);
```

### Issue: Image Not Loading

**Check**: Verify image URL is absolute
```javascript
// ✅ Works
"image": "https://example.com/product.jpg"

// ❌ Fails
"image": "/assets/product.jpg"  // Relative path
```

### Issue: Price Formatting Wrong

**Check**: Price should be in base units (VND)
```javascript
// ✅ Correct (formatted as 999,999 in display)
"price": 999999

// ❌ Wrong (formatted as 9999.99)
"price": "9999.99"
```

---

## Field Validation Checklist

### Product Data
- ✅ `id` - required, unique string
- ✅ `name` - required, visible text
- ✅ `image` - optional, absolute URL
- ✅ `price` - optional, number in VND
- ✅ `category` - optional, string
- ✅ `rating` - optional, 1-5 number

### Category Data
- ✅ `id` - required, unique string
- ✅ `name` - required, visible text
- ✅ `image` - optional, absolute URL
- ✅ `description` - optional, text
- ✅ `children` - optional, array of objects

### Brand Data
- ✅ `id` - required, unique string
- ✅ `name` - required, visible text
- ✅ `logo` - optional, absolute URL
- ✅ `description` - optional, text
- ✅ `country` - optional, country name

### Vendor Data
- ✅ `id` - required, unique string
- ✅ `name` - required, visible text
- ✅ `logo` - optional, absolute URL
- ✅ `description` - optional, text
- ✅ `rating` - optional, 1-5 number
