import { useState, useEffect, useRef } from 'react';
import './SearchableSelect.css';

/**
 * Generic SearchableSelect component
 * @param {Object} props
 * @param {string} props.value - Selected value
 * @param {Function} props.onChange - Callback function when value changes
 * @param {Function} props.onSearch - Async function to fetch items based on search term
 * @param {Array} props.items - Array of items to display (when not using onSearch)
 * @param {string} props.itemValueKey - Key to use for option value (default: 'id')
 * @param {string} props.itemLabelKey - Key to use for option label (default: 'name')
 * @param {Function} props.renderItem - Custom render function for each item
 * @param {string} props.placeholder - Placeholder text
 * @param {string} props.error - Error message
 * @param {boolean} props.disabled - Disabled state
 * @param {number} props.debounceMs - Debounce time in milliseconds (default: 800)
 * @param {boolean} props.clearable - Show clear button (default: true)
 */
export default function SearchableSelect({
  value,
  onChange,
  onSearch,
  items = [],
  itemValueKey = 'id',
  itemLabelKey = 'name',
  renderItem,
  placeholder = '-- Select --',
  error,
  disabled = false,
  debounceMs = 800,
  clearable = true,
  initialSelectedItem = null,
}) {
  const [isOpen, setIsOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [displayItems, setDisplayItems] = useState(items);
  const [loading, setLoading] = useState(false);
  const [selectedItem, setSelectedItem] = useState(null);
  const debounceTimerRef = useRef(null);
  const containerRef = useRef(null);
  const hasInitialLoadRef = useRef(false); // Track initial load

  // Keep the currently selected item available even when options are loaded
  // lazily through onSearch.
  useEffect(() => {
    if (!initialSelectedItem) return;
    setDisplayItems((current) => {
      const exists = current.some(
        (item) => item[itemValueKey] === initialSelectedItem[itemValueKey]
      );
      return exists ? current : [initialSelectedItem, ...current];
    });
  }, [initialSelectedItem, itemValueKey]);

  // Load initial items once when dropdown opens (if using onSearch)
  useEffect(() => {
    if (onSearch && isOpen && !hasInitialLoadRef.current) {
      hasInitialLoadRef.current = true;
      const loadInitial = async () => {
        try {
          setLoading(true);
          const result = await onSearch('');
          setDisplayItems(result);
        } catch (err) {
          console.error('Failed to load items:', err);
          setDisplayItems([]);
        } finally {
          setLoading(false);
        }
      };
      loadInitial();
    }
  }, [isOpen, onSearch]);

  // Update display items when items prop changes (for local items)
  useEffect(() => {
    if (!onSearch) {
      setDisplayItems(items);
    }
  }, [items, onSearch]);

  // Handle local filtering for non-search mode
  useEffect(() => {
    if (onSearch) {
      return; // Skip if using onSearch (handled by debounced search effect)
    }

    // Filter local items based on searchTerm
    if (!searchTerm.trim()) {
      setDisplayItems(items);
      return;
    }

    const filtered = items.filter((item) =>
      String(item[itemLabelKey]).toLowerCase().includes(searchTerm.toLowerCase())
    );
    setDisplayItems(filtered);
  }, [searchTerm, items, itemLabelKey, onSearch]);

  // Set selected item when value changes
  useEffect(() => {
    if (value) {
      const item = displayItems.find((item) => item[itemValueKey] === value);
      setSelectedItem(item || null);
    } else {
      setSelectedItem(null);
    }
  }, [value, displayItems, itemValueKey]);

  // Debounced search
  useEffect(() => {
    // Only apply debounce when searching (not on initial load)
    if (!onSearch || !searchTerm.trim()) {
      return;
    }

    // Clear any pending debounce timer
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
    }

    // Set new debounce timer
    debounceTimerRef.current = setTimeout(async () => {
      try {
        setLoading(true);
        const result = await onSearch(searchTerm);
        setDisplayItems(result);
      } catch (err) {
        console.error('Failed to search items:', err);
        setDisplayItems([]);
      } finally {
        setLoading(false);
      }
    }, debounceMs);

    return () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }
    };
  }, [searchTerm, onSearch, debounceMs]);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSelect = (item) => {
    onChange({ target: { name: 'value', value: item[itemValueKey] } });
    setSelectedItem(item);
    setIsOpen(false);
    setSearchTerm('');
  };

  const handleClear = (e) => {
    e.stopPropagation();
    onChange({ target: { name: 'value', value: '' } });
    setSelectedItem(null);
    setSearchTerm('');
  };

  const getItemLabel = (item) => {
    return String(item[itemLabelKey]);
  };

  return (
    <div className="searchable-select-wrapper" ref={containerRef}>
      <div
        className={`searchable-select-header ${error ? 'error' : ''} ${
          disabled ? 'disabled' : ''
        }`}
        onClick={() => !disabled && setIsOpen(!isOpen)}
      >
        <input
          type="text"
          placeholder={selectedItem ? getItemLabel(selectedItem) : placeholder}
          value={isOpen ? searchTerm : ''}
          onChange={(e) => {
            setSearchTerm(e.target.value);
            setIsOpen(true);
          }}
          onClick={(e) => {
            e.stopPropagation();
            setIsOpen(true);
          }}
          disabled={disabled}
          className="search-input"
        />
        {selectedItem && clearable && (
          <button
            type="button"
            className="clear-btn"
            onClick={handleClear}
            title="Clear selection"
          >
            <i className="fa-solid fa-xmark"></i>
          </button>
        )}
        <i className={`fa-solid fa-chevron-down chevron ${isOpen ? 'open' : ''}`}></i>
      </div>

      {isOpen && (
        <div className="searchable-select-dropdown">
          {loading ? (
            <div className="loading-state">
              <i className="fa-solid fa-spinner fa-spin"></i> Loading...
            </div>
          ) : displayItems.length === 0 ? (
            <div className="empty-state">
              {searchTerm ? 'No results found' : 'No items available'}
            </div>
          ) : (
            <ul className="items-list">
              {displayItems.map((item) => (
                <li
                  key={item[itemValueKey]}
                  className={`item-row ${
                    selectedItem?.[itemValueKey] === item[itemValueKey] ? 'selected' : ''
                  }`}
                  onClick={() => handleSelect(item)}
                >
                  {renderItem ? (
                    renderItem(item)
                  ) : (
                    <span className="item-label">{getItemLabel(item)}</span>
                  )}
                </li>
              ))}
            </ul>
          )}
        </div>
      )}

      {error && <span className="error-message">{error}</span>}
    </div>
  );
}
