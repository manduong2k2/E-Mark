import React from 'react';
import {
  ProductCard,
  ProductList,
  CategoryCard,
  CategoryList,
  BrandCard,
  BrandList,
  VendorCard,
  VendorList,
  OrderCard,
  OrderList,
} from './index';

/**
 * Determines if data contains nested list structure
 * Returns true if data has a 'data' property that is an array
 */
const isListFormat = (data) => {
  return data && Array.isArray(data.data);
};

/**
 * Maps intent to the appropriate component
 */
const getComponent = (intent, data) => {
  if (!intent || !data) return null;

  const intentLower = intent.toLowerCase();

  // Product components
  if (intentLower === 'product' || intentLower === 'products') {
    return isListFormat(data) ? ProductList : ProductCard;
  }

  // Category components
  if (intentLower === 'category' || intentLower === 'categories') {
    return isListFormat(data) ? CategoryList : CategoryCard;
  }

  // Brand components
  if (intentLower === 'brand' || intentLower === 'brands') {
    return isListFormat(data) ? BrandList : BrandCard;
  }

  // Vendor components
  if (intentLower === 'vendor' || intentLower === 'vendors') {
    return isListFormat(data) ? VendorList : VendorCard;
  }

  // Order components
  if (intentLower === 'order' || intentLower === 'orders') {
    return isListFormat(data) ? OrderList : OrderCard;
  }

  return null;
};

/**
 * Renders the appropriate component based on the intent and data format
 */
const AttachmentRenderer = ({ intent, data }) => {
  const Component = getComponent(intent, data);

  if (!Component) {
    console.warn(`Unknown intent or no component for: ${intent}`);
    return null;
  }

  // Pass the full data object to List components, single item to Card components
  const props = isListFormat(data) ? { data } : { [intent.toLowerCase().replace(/s$/, '')]: data };

  return <Component {...props} />;
};

export default AttachmentRenderer;
