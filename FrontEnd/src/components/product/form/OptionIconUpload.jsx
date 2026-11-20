import { useEffect, useRef, useState } from 'react';

export default function OptionIconUpload({ id, file, onChange, error, disabled, optionName }) {
  const inputRef = useRef(null);
  const [dragging, setDragging] = useState(false);
  const [uploadError, setUploadError] = useState('');
  const [preview, setPreview] = useState(null);
  const message = uploadError || error;

  useEffect(() => {
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => setPreview({ file, url: reader.result });
    reader.readAsDataURL(file);
    return () => {
      if (reader.readyState === FileReader.LOADING) reader.abort();
    };
  }, [file]);

  const selectFiles = (files) => {
    if (disabled || !files.length) return;
    if (files.length > 1) {
      setUploadError('Choose one image for the option icon');
      return;
    }
    const selectedFile = files[0];
    if (!selectedFile.type.startsWith('image/') || selectedFile.size > 5 * 1024 * 1024) {
      setUploadError('Choose an image up to 5MB');
      return;
    }
    setUploadError('');
    onChange(selectedFile);
  };

  return (
    <div className="product-option-icon-upload">
      <button type="button" disabled={disabled}
        className={`upload-placeholder product-option-icon-dropzone${dragging ? ' drag-over' : ''}${message ? ' error' : ''}`}
        aria-label={`${file ? 'Replace' : 'Upload'} icon for ${optionName}`}
        aria-invalid={Boolean(message)} aria-describedby={message ? `${id}-error` : `${id}-hint`}
        onClick={() => inputRef.current?.click()}
        onDragOver={event => {
          event.preventDefault();
          if (!disabled) {
            event.dataTransfer.dropEffect = 'copy';
            setDragging(true);
          }
        }}
        onDragLeave={event => {
          if (!event.currentTarget.contains(event.relatedTarget)) setDragging(false);
        }}
        onDrop={event => {
          event.preventDefault();
          event.stopPropagation();
          setDragging(false);
          selectFiles(event.dataTransfer.files);
        }}>
        {file && preview?.file === file ? (
          <img src={preview.url} alt={`Icon preview for ${optionName}`} className="product-option-icon-preview" />
        ) : (
          <span className="upload-icon"><i className="fa-solid fa-cloud-arrow-up" aria-hidden="true"></i></span>
        )}
        <span className="product-option-upload-title">{file ? file.name : 'Click or drag an image here'}</span>
        <small id={`${id}-hint`}>{file ? 'Click or drop another image to replace · Max 5MB' : 'One image · Max 5MB'}</small>
      </button>
      {file && (
        <button type="button" className="image-remove-btn product-option-icon-remove" disabled={disabled}
          aria-label={`Remove icon for ${optionName}`} onClick={() => {
            setUploadError('');
            onChange(null);
          }}>
          <i className="fa-solid fa-trash-can" aria-hidden="true"></i>
        </button>
      )}
      <input id={id} ref={inputRef} className="hidden-file-input" type="file" accept="image/*"
        disabled={disabled} tabIndex={-1} aria-label={`Icon file for ${optionName}`}
        onChange={event => {
          selectFiles(event.target.files);
          event.target.value = '';
        }} />
      {message && <span id={`${id}-error`} className="error-message" role="alert">{message}</span>}
    </div>
  );
}
