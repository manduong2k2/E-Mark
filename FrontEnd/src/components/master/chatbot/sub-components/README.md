# Chatbot Sub-Components

This folder contains mini-components for rendering different types of data returned by the AI chatbot API in response to user queries.

## Structure

```
sub-components/
├── product/
│   ├── ProductCard.jsx      # Single product detail
│   ├── ProductCard.css
│   ├── ProductList.jsx      # List of products with pagination
│   └── ProductList.css
├── category/
│   ├── CategoryCard.jsx     # Single category detail
│   ├── CategoryCard.css
│   ├── CategoryList.jsx     # List of categories with pagination
│   └── CategoryList.css
├── brand/
│   ├── BrandCard.jsx        # Single brand detail
│   ├── BrandCard.css
│   ├── BrandList.jsx        # List of brands with pagination
│   └── BrandList.css
├── vendor/
│   ├── VendorCard.jsx       # Single vendor detail
│   ├── VendorCard.css
│   ├── VendorList.jsx       # List of vendors with pagination
│   └── VendorList.css
├── AttachmentRenderer.jsx   # Smart renderer that selects the right component
├── index.js                 # Exports all components
└── README.md               # This file
```

## How It Works

### API Response Format

The chatbot API returns responses in two formats:

#### Detail Format (Single Item)
```json
{
  "success": true,
  "data": {
    "reply": "Here is the category with ID...",
    "attachments": {
      "intent": "categories",
      "data": {
        "id": "01a1276e-93fc-7ba8-aa5b-88dd8084206d",
        "name": "Women Fashion",
        "image": "http://...",
        "description": "...",
        "children": []
      }
    }
  }
}
```

#### List Format (Multiple Items with Pagination)
```json
{
  "success": true,
  "data": {
    "reply": "Here are the top categories...",
    "attachments": {
      "intent": "categories",
      "data": {
        "currentPage": 0,
        "data": [
          { "id": "...", "name": "...", ... },
          { "id": "...", "name": "...", ... }
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

### Automatic Component Selection

The `AttachmentRenderer` component automatically:

1. **Reads the `intent`** - determines the type (product, category, brand, vendor)
2. **Detects the data format** - checks if `data.data` is an array
   - If array exists → uses **List component**
   - If no array → uses **Card component**
3. **Renders the appropriate component** with the data

### Intent Mapping

| Intent | Card Component | List Component |
|--------|---|---|
| `product`, `products` | ProductCard | ProductList |
| `category`, `categories` | CategoryCard | CategoryList |
| `brand`, `brands` | BrandCard | BrandList |
| `vendor`, `vendors` | VendorCard | VendorList |

## Component Features

### Card Components
- Display single item details
- Optimized for compact chat bubble display
- Show essential information: name, image, key stats
- Hover effects for better UX
- Max-width ~320px for chat context

### List Components
- Display multiple items in a scrollable container
- Show pagination info (current page / total pages)
- Render multiple card items
- Empty state handling
- Max-height 400px with scrollbar
- Same styling as card for consistency

### Sizing & Styling
- **Chat context**: Components fit within 380px chat window
- **Max list height**: 400px with scrollbar (doesn't overflow chat)
- **Responsive**: Adapts to mobile (reduced to window - 32px margin)
- **Colors**: Match the master color scheme (indigo/purple)
- **Accessibility**: Proper icons and semantic HTML

## Field Mappings

### Product
- `name` - Product name
- `image` - Product image URL
- `price` - Price (formatted as VND currency)
- `category` - Category name
- `rating` - Star rating

### Category
- `name` - Category name
- `image` - Category image URL
- `description` - Category description
- `children` - Array of sub-categories (shown as count)

### Brand
- `name` - Brand name
- `logo` - Brand logo URL
- `description` - Brand description
- `country` - Country of origin

### Vendor
- `name` - Vendor/Shop name
- `logo` - Vendor logo/avatar URL
- `description` - Vendor description
- `rating` - Shop rating

## Usage in Chatbot Component

The main `Chatbot.jsx` handles attachments like this:

```javascript
const response = await assistantService.chat(message, provider);
if (response.ok) {
  const reply = response.data?.data?.reply || '';
  const attachments = response.data?.data?.attachments;
  
  // Add text reply
  addMessage('bot', reply);
  
  // Add attachment component if present
  if (attachments && attachments.intent && attachments.data) {
    addMessage('bot', '', { 
      isAttachment: true,
      attachmentIntent: attachments.intent,
      attachmentData: attachments.data
    });
  }
}
```

In the message rendering:
```javascript
{msg.isAttachment ? (
  <div className="msg-attachment">
    <AttachmentRenderer 
      intent={msg.attachmentIntent} 
      data={msg.attachmentData} 
    />
  </div>
) : (
  // Regular text message
)}
```

## Adding New Entity Types

To add support for a new entity type (e.g., "Review"):

1. **Create folder structure**:
   ```
   review/
   ├── ReviewCard.jsx
   ├── ReviewCard.css
   ├── ReviewList.jsx
   └── ReviewList.css
   ```

2. **Implement components** following the same pattern as existing ones

3. **Export in index.js**:
   ```javascript
   export { default as ReviewCard } from './review/ReviewCard';
   export { default as ReviewList } from './review/ReviewList';
   ```

4. **Add mapping in AttachmentRenderer.jsx**:
   ```javascript
   if (intentLower === 'review' || intentLower === 'reviews') {
     return isListFormat(data) ? ReviewList : ReviewCard;
   }
   ```

## Styling Notes

- All components inherit color scheme from CSS variables
- `--master-color` and `--master-primary` are used for theming
- Components use compact margins and padding suitable for chat
- Hover states provide visual feedback
- Empty states have helpful icons and messages

## CSS Classes

Each component has scoped CSS with these naming patterns:
- `.chatbot-[type]-card` - Card container
- `.chatbot-[type]-list` - List container
- `.chatbot-[type]-[field]` - Specific field styling
- `.empty-state` - Empty list state
- `.list-pagination` - Pagination info

## Accessibility

- Semantic HTML structure
- Descriptive icon usage (FontAwesome icons with text labels)
- Proper color contrast
- Responsive design for all screen sizes
- Keyboard-friendly for future interactive features
