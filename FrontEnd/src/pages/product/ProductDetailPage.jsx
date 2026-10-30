import { useContext, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { productService } from '../../services/productService';
import { AuthContext } from '../../contexts/AuthContext';
import ProductVariantPopup from '../../components/product/popup/ProductVariantPopup';
import defaultProductImage from '../../assets/product.png';
import './ProductDetailPage.css';

const imageValue = (image) => typeof image === 'string' ? image : image?.url;
const money = (value) => `${Number(value || 0).toLocaleString('vi-VN')} đ`;

export default function ProductDetailPage() {
  const { id } = useParams(); const navigate = useNavigate(); const { user } = useContext(AuthContext);
  const [product, setProduct] = useState(null); const [selectedVariant, setSelectedVariant] = useState(null);
  const [activeImage, setActiveImage] = useState(0); const [loading, setLoading] = useState(true);
  const [error, setError] = useState(''); const [showPopup, setShowPopup] = useState(false);

  useEffect(() => { let mounted = true;
    productService.getById(id).then((res) => { if (!mounted) return;
      if (res.ok && res.data?.data) { setProduct(res.data.data); setSelectedVariant(res.data.data.variants?.[0] || null); }
      else setError('Product not found.');
    }).catch(() => mounted && setError('Unable to load product details.')).finally(() => mounted && setLoading(false));
    return () => { mounted = false; };
  }, [id]);

  const variants = product?.variants || [];
  const images = useMemo(() => [...new Set((selectedVariant?.images || variants.flatMap(v => v.images || [])).map(imageValue).filter(Boolean))], [selectedVariant, variants]);
  const currentImage = images[activeImage] || defaultProductImage;
  const totalStock = variants.reduce((sum, v) => sum + Number(v.stock || 0), 0);
  const prices = variants.map(v => Number(v.price)).filter(Number.isFinite);
  const minPrice = prices.length ? Math.min(...prices) : 0;
  useEffect(() => setActiveImage(0), [selectedVariant]);

  if (loading) return <div className="product-detail-page"><div className="product-detail-state"><span className="product-detail-spinner" /><p>Loading product...</p></div></div>;
  if (error || !product) return <div className="product-detail-page"><div className="product-detail-state"><i className="fa-solid fa-box-open" /><h2>{error || 'Product not found'}</h2><button onClick={() => navigate('/home')}>Back to home</button></div></div>;

  return <main className="product-detail-page">
    <div className="product-detail-breadcrumb"><button onClick={() => navigate(-1)}><i className="fa-solid fa-arrow-left" /> Back</button><span>/</span><span>{product.name}</span></div>
    <section className="product-detail-card">
      <div className="product-detail-gallery"><div className="product-detail-main-image"><img src={currentImage} alt={product.name} onError={e => { e.currentTarget.src = defaultProductImage; }} />{product.status === 'PUBLISHED' && <span className="product-detail-badge">Published</span>}</div>
        {images.length > 1 && <div className="product-detail-thumbnails">{images.map((image, index) => <button key={image} className={index === activeImage ? 'active' : ''} onClick={() => setActiveImage(index)}><img src={image} alt={`${product.name} ${index + 1}`} /></button>)}</div>}</div>
      <div className="product-detail-content"><div className="product-detail-eyebrow">{product.brand?.name || 'Sản phẩm'} {product.categories?.length > 0 && ` · ${product.categories[0].name}`}</div>
        <h1>{product.name}</h1><div className="product-detail-rating"><i className="fa-solid fa-star" /> Authentic product <span>•</span> {variants.length} variant{variants.length === 1 ? '' : 's'}</div>
        <div className="product-detail-price">{money(selectedVariant?.price ?? minPrice)}</div>
        <div className="product-detail-stock"><span className={totalStock > 0 ? 'in-stock' : 'out-stock'}><i className="fa-solid fa-circle" /> {totalStock > 0 ? `${totalStock} in stock` : 'Out of stock'}</span><span>SKU: {selectedVariant?.sku || '—'}</span></div>
        {variants.length > 1 && <div className="product-detail-variants"><h3>Select variant</h3><div>{variants.map(v => <button key={v.id} className={selectedVariant?.id === v.id ? 'active' : ''} onClick={() => setSelectedVariant(v)}><strong>{v.name}</strong><small>{money(v.price)} · {v.stock} available</small></button>)}</div></div>}
        <button className="product-detail-add" disabled={!selectedVariant || selectedVariant.stock <= 0} onClick={() => { if (!user) navigate('/auth?action=login'); else setShowPopup(true); }}><i className="fa-solid fa-bag-shopping" /> {selectedVariant?.stock > 0 ? 'Add to cart' : 'Out of stock'}</button>
        <div className="product-detail-benefits"><span><i className="fa-solid fa-shield-halved" /><b>Authentic</b><small>Quality guaranteed</small></span><span><i className="fa-solid fa-truck-fast" /><b>Fast delivery</b><small>Carefully packaged</small></span></div>
      </div>
    </section>
    <section className="product-detail-lower"><article><h2>Product information</h2><p>{product.description || 'No detailed description available.'}</p>{product.categories?.length > 0 && <div className="product-detail-tags">{product.categories.map(c => <span key={c.id}>{c.name}</span>)}</div>}</article>
      <aside><h2>Store information</h2><div className="product-detail-vendor"><img src={product.vendor?.logo || defaultProductImage} alt={product.vendor?.name || 'Vendor'} onError={e => { e.currentTarget.src = defaultProductImage; }} /><div><strong>{product.vendor?.name || 'Seller'}</strong><small>{product.vendor?.email || 'Email unavailable'}</small></div></div><p>{product.vendor?.description || 'A trusted seller on Marketplace.'}</p></aside></section>
    {showPopup && <ProductVariantPopup product={product} onClose={() => setShowPopup(false)} />}
  </main>;
}

