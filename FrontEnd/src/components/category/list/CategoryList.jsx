import { useEffect, useId, useRef, useState } from 'react';
import CategoryCard from '../card/CategoryCard';
import { categoryService } from '../../../services/categoryService';
import './CategoryList.css';

const normalize = (value) => value.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd');
const countCategories = (items) => items.reduce((total, item) => total + 1 + countCategories(item.children || []), 0);

function filterCategories(items, query) {
  return items.flatMap((item) => {
    if (normalize(item.name).includes(query)) return [item];
    const children = filterCategories(item.children || [], query);
    return children.length ? [{ ...item, children, subcategoryCount: item.children.length }] : [];
  });
}

function findParents(items, id, parents = []) {
  for (const item of items) {
    if (item.id === id) return parents;
    const found = findParents(item.children || [], id, [...parents, item.id]);
    if (found) return found;
  }
  return null;
}

export default function CategoryList({ selectedCategoryId, onSelectCategory }) {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [search, setSearch] = useState('');
  const [accordion, setAccordion] = useState(true);
  const [expandedIds, setExpandedIds] = useState(new Set());
  const [mobile, setMobile] = useState(() => window.matchMedia('(max-width: 768px)').matches);
  const [open, setOpen] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);
  const dialogRef = useRef(null);
  const searchRef = useRef(null);
  const titleId = useId();
  const searchId = useId();
  const dialogId = useId();

  useEffect(() => {
    let cancelled = false;
    async function fetchCategories() {
      try {
        const response = await categoryService.getAll();
        if (!response.ok) throw new Error('Cannot load categories');
        if (!cancelled) {
          setCategories((response.data?.data || []).filter((category) => category.parentId == null));
        }
      } catch {
        if (!cancelled) setError('Cannot load categories. Please try again.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    fetchCategories();
    return () => { cancelled = true; };
  }, [reloadKey]);

  useEffect(() => {
    const media = window.matchMedia('(max-width: 768px)');
    const handleChange = (event) => {
      setMobile(event.matches);
      setOpen(false);
    };
    media.addEventListener('change', handleChange);
    return () => media.removeEventListener('change', handleChange);
  }, []);

  useEffect(() => {
    if (!mobile || !open) return;
    const dialog = dialogRef.current;
    const previousOverflow = document.body.style.overflow;
    dialog.showModal();
    document.body.style.overflow = 'hidden';
    return () => {
      dialog.close();
      document.body.style.overflow = previousOverflow;
    };
  }, [mobile, open]);

  const query = normalize(search.trim());
  const visibleCategories = query ? filterCategories(categories, query) : categories;
  const total = countCategories(categories);

  const handleToggle = (id, siblings) => {
    setExpandedIds((previous) => {
      const next = new Set(previous);
      if (next.has(id)) {
        next.delete(id);
      } else {
        if (accordion) siblings.forEach((sibling) => next.delete(sibling.id));
        next.add(id);
      }
      return next;
    });
  };

  const handleSelect = (id) => {
    if (id != null) {
      const parents = findParents(categories, id) || [];
      setExpandedIds((previous) => new Set(accordion ? parents : [...previous, ...parents]));
    }
    onSelectCategory?.(id);
    setOpen(false);
  };

  const panel = (
    <>
      <header className="category-sidebar-header">
        <div className="category-sidebar-heading">
          <span className="category-sidebar-mark" aria-hidden="true">C</span>
          <h2 id={titleId}>Product categories</h2>
          {mobile && <button type="button" className="category-sidebar-close" onClick={() => setOpen(false)} aria-label="Close categories">×</button>}
        </div>
        <div className="category-sidebar-search">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
            <circle cx="10.5" cy="10.5" r="6.5" /><path d="m16 16 4.5 4.5" strokeLinecap="round" />
          </svg>
          <label htmlFor={searchId} className="category-sr-only">Search categories</label>
          <input ref={searchRef} id={searchId} type="search" placeholder="Search categories..." value={search} onChange={(event) => setSearch(event.target.value)} />
          {search && <button type="button" onClick={() => { setSearch(''); searchRef.current?.focus(); }} aria-label="Clear category search">×</button>}
        </div>
      </header>
      <div className="category-sidebar-controls">
        <span>View mode</span>
        <label className="category-sidebar-switch">
          <input type="checkbox" role="switch" checked={accordion} onChange={(event) => {
            setAccordion(event.target.checked);
            if (event.target.checked) setExpandedIds(new Set());
          }} aria-label="Accordion mode" />
          <span className="category-sidebar-switch-track" aria-hidden="true" />
          <span>{accordion ? 'Accordion' : 'Multi-open'}</span>
        </label>
      </div>
      <nav className="category-sidebar-nav" aria-label="Product categories" aria-busy={loading}>
        {loading ? (
          <div className="category-sidebar-state" role="status"><span className="category-sidebar-spinner" />Loading categories...</div>
        ) : error ? (
          <div className="category-sidebar-state" role="alert">
            <p>{error}</p>
            <button type="button" onClick={() => { setError(null); setLoading(true); setReloadKey((key) => key + 1); }}>Try again</button>
          </div>
        ) : (
          <>
            <div className={`category-tree-row${selectedCategoryId == null ? ' is-selected' : ''}`}>
              <button type="button" className="category-tree-select" aria-pressed={selectedCategoryId == null} onClick={() => handleSelect(null)}>
                <svg className="category-tree-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" aria-hidden="true">
                  <path d="m3 10 9-7 9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1Z" strokeLinejoin="round" />
                </svg>
                <span className="category-tree-name">All products</span>
              </button>
            </div>
            {visibleCategories.length ? (
              <ul className="category-sidebar-tree">
                {visibleCategories.map((category) => (
                  <CategoryCard key={category.id} category={category} selectedCategoryId={selectedCategoryId} onSelectCategory={handleSelect} expandedIds={expandedIds} onToggle={handleToggle} siblings={visibleCategories} searching={Boolean(query)} />
                ))}
              </ul>
            ) : (
              <div className="category-sidebar-state" role="status">
                <p>{categories.length ? 'No matching categories.' : 'No categories available yet.'}</p>
                {query && <button type="button" onClick={() => setSearch('')}>Clear search</button>}
              </div>
            )}
          </>
        )}
      </nav>
      <footer className="category-sidebar-footer">
        <span>Total categories: <strong>{loading ? '—' : total}</strong></span>
        <button type="button" disabled={loading || Boolean(query) || expandedIds.size === 0} onClick={() => setExpandedIds(new Set())}>Collapse all</button>
      </footer>
    </>
  );

  if (mobile) {
    return (
      <>
        <button type="button" className="category-sidebar-trigger" onClick={() => setOpen(true)} aria-haspopup="dialog" aria-controls={dialogId} aria-expanded={open}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true"><path d="M4 6h16M4 12h16M4 18h16" strokeLinecap="round" /></svg>
          Browse categories
          {selectedCategoryId != null && <span className="category-sidebar-filter-dot" aria-label="Category filter active" />}
        </button>
        <dialog ref={dialogRef} id={dialogId} className="category-sidebar category-sidebar-dialog" aria-labelledby={titleId} onCancel={() => setOpen(false)} onClick={(event) => { if (event.target === event.currentTarget) setOpen(false); }}>
          <div className="category-sidebar-panel">{panel}</div>
        </dialog>
      </>
    );
  }

  return <aside className="category-sidebar" aria-labelledby={titleId}>{panel}</aside>;
}
