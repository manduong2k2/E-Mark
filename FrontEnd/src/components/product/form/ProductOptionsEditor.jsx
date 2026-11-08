import { createProductOption, createOptionValue } from './productOptions';
import OptionIconUpload from './OptionIconUpload';

export default function ProductOptionsEditor({ attributes, onChange, errors, disabled }) {
  const updateOption = (index, changes) => {
    onChange(attributes.map((attribute, i) => i === index ? { ...attribute, ...changes } : attribute));
  };
  const error = path => errors[path] && <span id={`${path}-error`} className="error-message" role="alert">{errors[path]}</span>;

  return (
    <section className="form-section product-options" aria-labelledby="product-options-heading">
      <div className="section-header">
        <div>
          <h3 className="section-title" id="product-options-heading">
            <i className="fa-solid fa-sliders" aria-hidden="true"></i> Product Options
          </h3>
          <p className="section-subtitle">Add options such as Color or Size, then choose a value for each variant.</p>
        </div>
        <button type="button" className="btn btn-secondary btn-sm" disabled={disabled}
          onClick={() => onChange([...attributes, createProductOption()])}>
          <i className="fa-solid fa-plus" aria-hidden="true"></i> Add Option
        </button>
      </div>
      {error('attributes')}
      {!attributes.length && <p className="empty-message">No options yet. Add an option if your variants differ by color, size, or another feature.</p>}
      <div className="product-option-list">
        {attributes.map((attribute, index) => {
          const path = `attributes[${index}]`;
          const fieldId = field => `product-option-${attribute.tempId}-${field}`;
          return (
            <fieldset key={attribute.tempId} className="product-option-card" disabled={disabled}>
              <legend>Option {index + 1}{attribute.name.trim() ? `: ${attribute.name}` : ''}</legend>
              <div className="product-option-toolbar">
                <button type="button" className="btn btn-danger btn-sm"
                  aria-label={`Remove option ${index + 1}`}
                  onClick={() => onChange(attributes.filter((_, i) => i !== index))}>
                  <i className="fa-solid fa-trash-can" aria-hidden="true"></i> Remove Option
                </button>
              </div>
              <div className="form-grid form-grid-2col">
                <div className="form-group">
                  <label className="form-label" htmlFor={fieldId('name')}>Option Name <span className="required">*</span></label>
                  <input id={fieldId('name')} className={`form-input ${errors[`${path}.name`] ? 'error' : ''}`}
                    value={attribute.name} maxLength={100} placeholder="e.g. Color"
                    aria-invalid={Boolean(errors[`${path}.name`])}
                    aria-describedby={errors[`${path}.name`] ? `${path}.name-error` : undefined}
                    onChange={e => updateOption(index, { name: e.target.value })} />
                  {error(`${path}.name`)}
                </div>
                <div className="form-group">
                  <label className="form-label" htmlFor={fieldId('code')}>Option Code <span className="required">*</span></label>
                  <input id={fieldId('code')} className={`form-input ${errors[`${path}.code`] ? 'error' : ''}`}
                    value={attribute.code} maxLength={100} placeholder="e.g. color"
                    aria-invalid={Boolean(errors[`${path}.code`])}
                    aria-describedby={errors[`${path}.code`] ? `${path}.code-error` : undefined}
                    onChange={e => updateOption(index, { code: e.target.value })} />
                  {error(`${path}.code`)}
                </div>
                <div className="form-group form-group-full">
                  <label className="form-label" htmlFor={fieldId('description')}>Description <span className="product-option-optional">Optional</span></label>
                  <textarea id={fieldId('description')} className={`form-textarea ${errors[`${path}.description`] ? 'error' : ''}`}
                    value={attribute.description} rows={2} maxLength={500} placeholder="Describe this option"
                    onChange={e => updateOption(index, { description: e.target.value })} />
                  {error(`${path}.description`)}
                </div>
                <div className="form-group form-group-full">
                  <label className="form-label" htmlFor={fieldId('icon')}>Icon <span className="product-option-optional">Optional · image up to 5MB</span></label>
                  <OptionIconUpload id={fieldId('icon')} file={attribute.icon}
                    optionName={attribute.name.trim() || `option ${index + 1}`}
                    error={errors[`${path}.icon`]} disabled={disabled}
                    onChange={icon => updateOption(index, { icon })} />
                </div>
              </div>
              <div className="product-option-values">
                <div className="product-option-values-heading">
                  <span className="form-label">Values <span className="required">*</span></span>
                  <button type="button" className="btn btn-secondary btn-sm"
                    aria-label={`Add value to option ${index + 1}`}
                    onClick={() => updateOption(index, { values: [...attribute.values, createOptionValue()] })}>
                    <i className="fa-solid fa-plus" aria-hidden="true"></i> Add Value
                  </button>
                </div>
                {error(`${path}.values`)}
                {attribute.values.map((item, valueIndex) => {
                  const valuePath = `${path}.values[${valueIndex}].value`;
                  return (
                    <div className="product-option-value" key={item.tempId}>
                      <div className="form-group">
                        <label className="form-label" htmlFor={`product-option-value-${item.tempId}`}>Value {valueIndex + 1}</label>
                        <input id={`product-option-value-${item.tempId}`} value={item.value} maxLength={255}
                          placeholder={attribute.name ? `${attribute.name} value` : 'e.g. Red'}
                          className={`form-input ${errors[valuePath] ? 'error' : ''}`}
                          aria-invalid={Boolean(errors[valuePath])}
                          aria-describedby={errors[valuePath] ? `${valuePath}-error` : undefined}
                          onChange={e => updateOption(index, { values: attribute.values.map(value =>
                            value.tempId === item.tempId ? { ...value, value: e.target.value } : value) })} />
                        {error(valuePath)}
                      </div>
                      <button type="button" className="btn btn-danger btn-sm"
                        disabled={attribute.values.length === 1}
                        title={attribute.values.length === 1 ? 'At least one value is required' : 'Remove value'}
                        aria-label={`Remove value ${valueIndex + 1} from option ${index + 1}`}
                        onClick={() => updateOption(index, { values: attribute.values.filter(value => value.tempId !== item.tempId) })}>
                        <i className="fa-solid fa-trash-can" aria-hidden="true"></i>
                      </button>
                    </div>
                  );
                })}
              </div>
            </fieldset>
          );
        })}
      </div>
    </section>
  );
}
