import { useEffect, useState } from 'react';
import SearchableSelect from '../../master/input/select/SearchableSelect';
import { vendorService } from '../../../services/vendorService';

/**
 * Vendor-specific SearchableSelect component
 * Wrapper around generic SearchableSelect for vendor selection
 */
export default function SearchableVendorSelect({
  value,
  onChange,
  error,
  disabled = false,
}) {
  const [selectedVendor, setSelectedVendor] = useState(null);

  // The vendor list is loaded lazily by SearchableSelect. Load the current
  // vendor separately so an existing product can display its selected label
  // before the dropdown is opened.
  useEffect(() => {
    let cancelled = false;

    if (!value) {
      setSelectedVendor(null);
      return undefined;
    }

    vendorService.getById(value)
      .then((res) => {
        if (!cancelled) setSelectedVendor(res.data?.data || res.data || null);
      })
      .catch(() => {
        if (!cancelled) setSelectedVendor(null);
      });

    return () => { cancelled = true; };
  }, [value]);

  const handleSearch = async (searchTerm) => {
    try {
      const res = await vendorService.getAll(
        searchTerm ? { search: searchTerm } : {}
      );
      return res.data?.data || [];
    } catch (err) {
      console.error('Failed to search vendors:', err);
      return [];
    }
  };

  const handleChange = (e) => {
    // Map 'value' field name to 'vendorId' for ProductForm
    onChange({
      target: {
        name: 'vendorId',
        value: e.target.value,
      },
    });
  };

  const renderVendorItem = (vendor) => (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%' }}>
      <span className="vendor-name">{vendor.name}</span>
      {vendor.status && (
        <span className={`vendor-status vendor-status-${vendor.status.toLowerCase()}`}>
          {vendor.status}
        </span>
      )}
    </div>
  );

  return (
    <SearchableSelect
      value={value}
      initialSelectedItem={selectedVendor}
      onChange={handleChange}
      onSearch={handleSearch}
      itemValueKey="id"
      itemLabelKey="name"
      renderItem={renderVendorItem}
      placeholder="-- Select Vendor --"
      error={error}
      disabled={disabled}
      debounceMs={800}
      clearable={true}
    />
  );
}
