import { useId } from 'react';
import defaultCategoryImage from '../../../assets/category.png';
import './CategoryCard.css';

export default function CategoryCard({ category, depth = 0, selectedCategoryId, onSelectCategory, expandedIds, onToggle, siblings, searching = false }) {
  const childrenId = useId();
  const children = category.children || [];
  const hasChildren = children.length > 0;
  const subcategoryCount = category.subcategoryCount ?? children.length;
  const expanded = hasChildren && (searching || expandedIds.has(category.id));
  const selected = selectedCategoryId === category.id;

  return (
    <li className="category-branch">
      <div className={`category-tree-row${selected ? ' is-selected' : ''}`} style={{ '--category-depth': depth }}>
        <button type="button" className="category-tree-select" onClick={() => onSelectCategory?.(category.id)} aria-pressed={selected} title={category.name}>
          {depth === 0 ? (
            <img src={category.image || defaultCategoryImage} alt="" className="category-tree-icon" onError={(event) => {
              event.currentTarget.onerror = null;
              event.currentTarget.src = defaultCategoryImage;
            }} />
          ) : <span className="category-tree-dot" aria-hidden="true" />}
          <span className="category-tree-name">{category.name}</span>
        </button>
        {hasChildren && (
          <>
            <span className="category-tree-count" title={`${subcategoryCount} subcategories`}>{subcategoryCount}</span>
            <button type="button" className="category-tree-toggle" onClick={() => onToggle(category.id, siblings)} aria-label={`${expanded ? 'Collapse' : 'Expand'} ${category.name}`} aria-expanded={expanded} aria-controls={childrenId} disabled={searching}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                <path d="m9 5 7 7-7 7" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </button>
          </>
        )}
      </div>
      {hasChildren && (
        <ul id={childrenId} className="category-tree-children" hidden={!expanded}>
          {children.map((child) => (
            <CategoryCard key={child.id} category={child} depth={depth + 1} selectedCategoryId={selectedCategoryId} onSelectCategory={onSelectCategory} expandedIds={expandedIds} onToggle={onToggle} siblings={children} searching={searching} />
          ))}
        </ul>
      )}
    </li>
  );
}
