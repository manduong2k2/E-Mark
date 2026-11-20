// src/components/category/CategoryForm.jsx
import React, { useState, useRef } from 'react';
import './CategoryForm.css';

export default function CategoryForm({ category, categories = [], onSubmit, onCancel, loading = false }) {
  const [formData, setFormData] = useState({
    name: category?.name || '',
    description: category?.description || '',
    image: category?.image || '',
    parentId: category?.parentId || '',
  });

  const [errors, setErrors] = useState({});
  const [imagePreview, setImagePreview] = useState(category?.image || '');
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef(null);
  const dragDepthRef = useRef(0);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const selectImage = (file) => {
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      setErrors(prev => ({ ...prev, image: 'Please select an image file' }));
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setErrors(prev => ({ ...prev, image: 'Image size must not exceed 5MB' }));
      return;
    }

    const reader = new FileReader();
    reader.onloadend = () => {
      setImagePreview(reader.result);
      setFormData(prev => ({
        ...prev,
        image: reader.result,
        imageFile: file,
      }));
      setErrors(prev => ({ ...prev, image: '' }));
    };
    reader.readAsDataURL(file);
  };

  const handleImageChange = (e) => {
    selectImage(e.target.files[0]);
    e.target.value = '';
  };

  const handleDragEnter = (e) => {
    e.preventDefault();
    e.stopPropagation();
    dragDepthRef.current += 1;
    setIsDragging(true);
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
    e.dataTransfer.dropEffect = 'copy';
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    e.stopPropagation();
    dragDepthRef.current = Math.max(0, dragDepthRef.current - 1);
    if (dragDepthRef.current === 0) setIsDragging(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    dragDepthRef.current = 0;
    setIsDragging(false);
    selectImage(e.dataTransfer.files[0]);
  };

  const validateForm = () => {
    const newErrors = {};
    if (!formData.name.trim()) newErrors.name = 'Category name is required';
    if (!formData.image) newErrors.image = 'Please select a category image';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;
    const serverErrors = await onSubmit(formData);
    if (serverErrors && typeof serverErrors === 'object') {
      if (serverErrors._) window.showError(serverErrors._);
      const { _: _ignored, ...fieldErrors } = serverErrors;
      if (Object.keys(fieldErrors).length > 0) {
        setErrors((prev) => ({ ...prev, ...fieldErrors }));
      }
    }
  };

  const handleReset = () => {
    setFormData({ name: '', description: '', image: '', parentId: '' });
    setImagePreview('');
    setIsDragging(false);
    dragDepthRef.current = 0;
    setErrors({});
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  return (
    <div className="category-form-container">
      <form onSubmit={handleSubmit} className="category-form">
        <h2>{category ? 'Edit Category' : 'Add New Category'}</h2>

        <div className="form-columns">
          {/* Left Column - Basic Information */}
          <div className="form-column-left">
            <div className="form-group">
              <label htmlFor="cat-name">Category Name *</label>
              <input
                type="text"
                id="cat-name"
                name="name"
                value={formData.name}
                onChange={handleChange}
                className={errors.name ? 'error' : ''}
                placeholder="Enter category name"
              />
              {errors.name && <span className="error-message">{errors.name}</span>}
            </div>

            <div className="form-group">
              <label htmlFor="cat-parent">Parent Category</label>
              <select
                id="cat-parent"
                name="parentId"
                value={formData.parentId}
                onChange={handleChange}
                className="form-select"
              >
                <option value="">-- None (top-level) --</option>
                {categories.map((cat) => (
                  <option key={cat.id} value={cat.id}>
                    {cat.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label htmlFor="cat-description">Description</label>
              <textarea
                id="cat-description"
                name="description"
                value={formData.description}
                onChange={handleChange}
                placeholder="Enter category description"
                rows="4"
                className="form-textarea"
              />
            </div>
          </div>

          {/* Right Column - Image */}
          <div className="form-column-right">
            <div className="form-group">
              <label>Category Image *</label>
              {/* Hidden real file input */}
              <input
                ref={fileInputRef}
                type="file"
                id="cat-image"
                accept="image/*"
                onChange={handleImageChange}
                className="image-input-hidden"
              />

              <div
                className={`image-upload-container${isDragging ? ' is-dragging' : ''}`}
                onDragEnter={handleDragEnter}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
              >
                {imagePreview ? (
                  <div className="image-preview">
                    <img src={imagePreview} alt="Preview" className="preview-image" draggable={false} />
                    <button
                      type="button"
                      className="remove-image-btn"
                      onClick={() => {
                        setImagePreview('');
                        setFormData(prev => ({ ...prev, image: '', imageFile: undefined }));
                        if (fileInputRef.current) fileInputRef.current.value = '';
                      }}
                    >
                      ×
                    </button>
                  </div>
                ) : (
                  <div
                    className="upload-placeholder"
                    onClick={() => fileInputRef.current?.click()}
                    role="button"
                    tabIndex={0}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        fileInputRef.current?.click();
                      }
                    }}
                  >
                    <div className="upload-icon">📷</div>
                    <p>Click or drag and drop an image</p>
                    <small>Supported: JPG, PNG, GIF (max 5MB)</small>
                  </div>
                )}
              </div>
              {errors.image && <span className="error-message">{errors.image}</span>}
            </div>
          </div>
        </div>

        <div className="form-actions">
          <button type="button" onClick={handleReset} className="btn btn-secondary" disabled={loading}>
            Reset
          </button>
          <button type="button" onClick={onCancel} className="btn btn-cancel" disabled={loading}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Saving...' : category ? 'Update' : 'Create'}
          </button>
        </div>
      </form>
    </div>
  );
}
