# Chatbot Attachment Components - Integration Guide

## Overview

The chatbot now automatically renders structured data (products, categories, brands, vendors) returned by the AI API in dedicated mini-components that fit perfectly within the chat interface.

## What Changed

### Before
- The chatbot only displayed text replies from the API
- No visual representation of products, categories, etc.
- Limited user interaction with API responses

### After
- **Smart Attachment Rendering**: Automatically detects and renders structured data
- **Two Formats Supported**:
  - **Card View**: Single item details (product, category, brand, vendor)
  - **List View**: Multiple items with pagination
- **Auto-Detection**: Determines which format based on data structure
- **Compact Design**: Components sized to fit 380px chat window

## API Response Format

Your API should return responses in this format:

```javascript
{
  success: true,
  data: {
    reply: "Your text response here",
    attachments: {
      intent: "products" | "categories" | "brands" | "vendors",
      data: { /* see examples below */ }
    }
  }
}
```

## Examples

### Example 1: Single Product Detail
```json
{
  "success": true,
  "data": {
    "reply": "Here's the iPhone 15 Pro we have in stock!",
    "attachments": {
      "intent": "product",
      "data": {
        "id": "123",
        "name": "iPhone 15 Pro",
        "image": "https://...",
        "price": 999999,
        "category": "Smartphones",
        "rating": 4.8
      }
    }
  }
}
```

**Result**: Renders ProductCard with image, name, price, category, and rating

---

### Example 2: Multiple Products
```json
{
  "success": true,
  "data": {
    "reply": "Here are the trending products this week:",
    "attachments": {
      "intent": "products",
      "data": {
        "currentPage": 0,
        "data": [
          { "id": "1", "name": "Product 1", "image": "...", "price": 100000, ... },
          { "id": "2", "name": "Product 2", "image": "...", "price": 200000, ... },
          { "id": "3", "name": "Product 3", "image": "...", "price": 150000, ... }
        ],
        "pageSize": 3,
        "totalElements": 15,
        "totalPages": 5,
        "hasNext": true,
        "hasPrevious": false
      }
    }
  }
}
```

**Result**: Renders ProductList showing 3 products in a scrollable container with pagination info

---

### Example 3: Single Category
```json
{
  "success": true,
  "data": {
    "reply": "Let me show you the Women Fashion category:",
    "attachments": {
      "intent": "category",
      "data": {
        "id": "cat-123",
        "name": "Women Fashion",
        "image": "https://...",
        "description": "Latest women's fashion collection",
        "children": [
          { "id": "sub1", "name": "Dresses", ... },
          { "id": "sub2", "name": "Tops", ... }
        ]
      }
    }
  }
}
```

**Result**: Renders CategoryCard showing the category with sub-categories count

---

### Example 4: Multiple Categories
```json
{
  "success": true,
  "data": {
    "reply": "Here are the top categories:",
    "attachments": {
      "intent": "categories",
      "data": {
        "currentPage": 0,
        "data": [
          { "id": "1", "name": "Fashion", "image": "...", ... },
          { "id": "2", "name": "Electronics", "image": "...", ... },
          { "id": "3", "name": "Home", "image": "...", ... }
        ],
        "totalPages": 2,
        "totalElements": 4,
        "pageSize": 3,
        "hasNext": true,
        "hasPrevious": false
      }
    }
  }
}
```

**Result**: Renders CategoryList with scrollable categories and pagination

---

### Example 5: Brand
```json
{
  "success": true,
  "data": {
    "reply": "Here's a premium brand we carry:",
    "attachments": {
      "intent": "brand",
      "data": {
        "id": "brand-1",
        "name": "Nike",
        "logo": "https://...",
        "description": "Leading athletic brand",
        "country": "USA"
      }
    }
  }
}
```

**Result**: Renders BrandCard with logo, name, description, and country

---

### Example 6: Vendor/Shop
```json
{
  "success": true,
  "data": {
    "reply": "This vendor has great reviews:",
    "attachments": {
      "intent": "vendor",
      "data": {
        "id": "vendor-1",
        "name": "TechStore 24",
        "logo": "https://...",
        "description": "Premium tech products",
        "rating": 4.9
      }
    }
  }
}
```

**Result**: Renders VendorCard with shop info and rating

## Intent Field Mapping

### Products
- `id` - Unique identifier
- `name` - Product name (required)
- `image` - Product image URL
- `price` - Price in VND (formatted automatically)
- `category` - Category name
- `rating` - Star rating (1-5)

### Categories
- `id` - Unique identifier
- `name` - Category name (required)
- `image` - Category image URL
- `description` - Category description
- `children` - Array of sub-categories (shown as count)

### Brands
- `id` - Unique identifier
- `name` - Brand name (required)
- `logo` - Brand logo URL
- `description` - Brand description
- `country` - Country of origin

### Vendors
- `id` - Unique identifier
- `name` - Vendor name (required)
- `logo` - Vendor logo/avatar URL
- `description` - Vendor description
- `rating` - Shop rating (1-5)

## List Response Requirements

For list responses, the data structure must include:

```javascript
{
  data: [],              // Array of items (required for list detection)
  currentPage: 0,        // Current page number (0-indexed)
  totalPages: 5,         // Total number of pages
  totalElements: 25,     // Total items across all pages
  pageSize: 5,           // Items per page
  hasNext: true,         // Whether there's a next page
  hasPrevious: false     // Whether there's a previous page
}
```

## Component Sizing

All components are optimized for the chat window:

- **Chat Window**: 380px wide × 580px tall
- **Max Component Width**: 320px (leaves room for spacing)
- **List Max Height**: 400px (scrollable if longer)
- **Mobile**: Adapts to window - 32px margin

## Styling

Components use the existing color scheme:
- **Primary Colors**: Indigo/Purple (matching chatbot theme)
- **Backgrounds**: White cards on light backgrounds
- **Icons**: FontAwesome icons for visual recognition
- **Hover Effects**: Subtle shadow and border color change

## Error Handling

If there's an issue:

### No Attachment
```javascript
// If attachments is missing, only text displays
{
  success: true,
  data: {
    reply: "Here's my response",
    attachments: null  // or omitted
  }
}
```

### Unknown Intent
```javascript
// Component logs a warning and renders nothing
// The text reply still displays
{
  data: {
    reply: "...",
    attachments: {
      intent: "unknown_type",  // Not recognized
      data: {}
    }
  }
}
```

### Empty List
```javascript
// Component shows an empty state with icon
{
  data: {
    data: [],  // Empty array
    totalElements: 0,
    totalPages: 0,
    ...
  }
}
```

## Testing

### Test Case 1: Single Product
Send: "Show me an iPhone"
Expected: Text reply + ProductCard component

### Test Case 2: Product List
Send: "What are the trending products?"
Expected: Text reply + ProductList component

### Test Case 3: Category
Send: "Tell me about women's fashion"
Expected: Text reply + CategoryCard component

### Test Case 4: Text Only
Send: "How do I track my order?"
Expected: Text reply only (no attachment)

## Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| Component not showing | Missing `attachments` or empty `data` | Verify API returns correct structure |
| Wrong component renders | `intent` value not recognized | Check intent spelling (case-insensitive) |
| List shows single card | `data.data` is not an array | Ensure list format has nested `data` array |
| Styling looks off | CSS not loaded | Check import path in Chatbot.jsx |
| No images display | Image URL broken | Validate image URLs in API response |
| Component text wraps oddly | Field text too long | Add ellipsis or truncation in components |

## Future Enhancements

Possible additions:
- Click handlers to navigate to product/category pages
- "Add to cart" buttons on products
- More detailed product specs
- Search filters within lists
- Sorting options
- Comparison view for multiple products
- Reviews and ratings display
- Stock availability indicator

## File Locations

- **Main Component**: `src/components/master/chatbot/Chatbot.jsx`
- **Sub-components**: `src/components/master/chatbot/sub-components/`
- **Renderer**: `src/components/master/chatbot/sub-components/AttachmentRenderer.jsx`
- **CSS**: Individual CSS files in each entity folder

## Support

For issues or questions:
1. Check the README.md in sub-components folder
2. Review example responses above
3. Verify API response structure matches format
4. Check browser console for warnings
