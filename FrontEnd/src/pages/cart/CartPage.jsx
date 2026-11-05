import { useContext, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { CartContext } from '../../contexts/CartContext';
import { cartService } from '../../services/cartService';
import { showSuccess, showError } from '../../components/master/popup';
import { Page, PageHeader, Card, ConfirmDialog, EmptyState, QuantityStepper, SkeletonRows, Thumb } from '../../components/ui/Ui';
import { formatMoney } from '../../components/ui/uiUtils';
import './CartPage.css';

export default function CartPage() {
  useEffect(() => {
    document.title = 'My Store - Cart';
  }, []);

  const navigate = useNavigate();
  const { cart, loading, setCart } = useContext(CartContext);
  const [busyId, setBusyId] = useState(null);
  const [confirmClear, setConfirmClear] = useState(false);
  const [clearing, setClearing] = useState(false);

  const reloadCart = async () => {
    const res = await cartService.getCart();
    if (res.ok) setCart(res.data?.cart ?? null);
  };

  const changeQuantity = async (item, quantity) => {
    if (quantity < 1 || quantity === item.quantity) return;
    setBusyId(item.id);
    try {
      const res = await cartService.updateItem(item.productVariantId, quantity);
      if (res.ok) await reloadCart();
      else showError(res.data?.message || 'Could not update the quantity', 'Cart');
    } catch {
      showError('Could not update the quantity. Please try again.', 'Connection Error');
    } finally {
      setBusyId(null);
    }
  };

  const removeItem = async (item) => {
    setBusyId(item.id);
    try {
      const res = await cartService.removeItem(item.productVariantId);
      if (res.ok) {
        await reloadCart();
        showSuccess(`${item.productName} removed from your cart`, 'Cart');
      } else {
        showError(res.data?.message || 'Could not remove the item', 'Cart');
      }
    } catch {
      showError('Could not remove the item. Please try again.', 'Connection Error');
    } finally {
      setBusyId(null);
    }
  };

  const clearCart = async () => {
    setClearing(true);
    try {
      const res = await cartService.clearCart();
      if (res.ok) {
        await reloadCart();
        showSuccess('Your cart is now empty', 'Cart');
      } else {
        showError(res.data?.message || 'Could not clear the cart', 'Cart');
      }
    } catch {
      showError('Could not clear the cart. Please try again.', 'Connection Error');
    } finally {
      setClearing(false);
      setConfirmClear(false);
    }
  };

  const items = cart?.items || [];
  const unitCount = items.reduce((sum, item) => sum + item.quantity, 0);

  return (
    <Page>
      <PageHeader
        eyebrow="Shopping"
        eyebrowIcon="bi-bag"
        title="Your cart"
        description={items.length ? `${unitCount} item${unitCount > 1 ? 's' : ''} ready for checkout.` : undefined}
        actions={items.length > 0 && (
          <button type="button" className="ui-btn ui-btn--danger-text" onClick={() => setConfirmClear(true)}>
            <i className="bi bi-trash"></i> Clear cart
          </button>
        )}
      />

      {loading ? (
        <Card bodyless><SkeletonRows rows={3} thumb /></Card>
      ) : items.length === 0 ? (
        <Card bodyless>
          <EmptyState icon="bi-bag" title="Your cart is empty" text="Browse the store and add products you like — they will show up here.">
            <Link to="/home" className="ui-btn ui-btn--primary"><i className="bi bi-shop"></i> Continue shopping</Link>
          </EmptyState>
        </Card>
      ) : (
        <div className="ui-split">
          <Card bodyless>
            <ul className="ui-list">
              {items.map(item => (
                <li key={item.id} className="ui-row cart-row">
                  <Thumb src={item.productImages?.[0]} />
                  <div className="ui-row-main">
                    <p className="ui-row-title">{item.productName}</p>
                    <p className="ui-row-sub">{formatMoney(item.productPrice)} each</p>
                  </div>
                  <div className="cart-row-actions">
                    <QuantityStepper
                      value={item.quantity}
                      onChange={(quantity) => changeQuantity(item, quantity)}
                      disabled={busyId === item.id}
                    />
                    <span className="ui-price cart-subtotal">{formatMoney(item.subTotal)}</span>
                    <button
                      type="button"
                      className="ui-icon-btn ui-icon-btn--danger"
                      onClick={() => removeItem(item)}
                      disabled={busyId === item.id}
                      aria-label={`Remove ${item.productName}`}
                      title="Remove"
                    >
                      <i className="bi bi-trash"></i>
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          </Card>

          <aside className="ui-sticky">
            <Card icon="bi-receipt" title="Order summary">
              <dl className="ui-kv">
                <div><dt>Items</dt><dd>{unitCount}</dd></div>
                <div><dt>Subtotal</dt><dd>{formatMoney(cart.total)}</dd></div>
                <div><dt>Shipping</dt><dd className="ui-muted">Calculated at checkout</dd></div>
                <div className="ui-kv-total"><dt>Total</dt><dd>{formatMoney(cart.total)}</dd></div>
              </dl>
              <button type="button" className="ui-btn ui-btn--primary ui-btn--lg ui-btn--block cart-checkout" onClick={() => navigate('/checkout')}>
                <i className="bi bi-credit-card"></i> Proceed to checkout
              </button>
              <Link to="/home" className="ui-btn ui-btn--ghost ui-btn--block">Continue shopping</Link>
            </Card>
          </aside>
        </div>
      )}

      {confirmClear && (
        <ConfirmDialog
          title="Clear your cart?"
          message="All products will be removed from your cart."
          confirmLabel="Clear cart"
          busy={clearing}
          onConfirm={clearCart}
          onClose={() => setConfirmClear(false)}
        />
      )}
    </Page>
  );
}
