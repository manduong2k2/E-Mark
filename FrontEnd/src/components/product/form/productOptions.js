let nextTempId = Date.now();

// Values share one sequence across all options: the API references them by tempId.
export function createOptionValue() {
  return { tempId: ++nextTempId, value: '' };
}

export function createProductOption() {
  return {
    tempId: ++nextTempId,
    code: '',
    name: '',
    description: '',
    icon: null,
    values: [createOptionValue()],
  };
}

export function retainOptionSelections(variants, attributes) {
  const availableIds = new Set(attributes.flatMap(attribute => attribute.values.map(value => value.tempId)));
  return variants.map(variant => ({
    ...variant,
    attributeValueIds: (variant.attributeValueIds || []).filter(id => availableIds.has(id)),
  }));
}

export function selectOptionValue(selectedIds, attribute, selectedValue) {
  const optionIds = new Set(attribute.values.map(value => value.tempId));
  const otherSelections = selectedIds.filter(id => !optionIds.has(id));
  return selectedValue === '' ? otherSelections : [...otherSelections, Number(selectedValue)];
}

export function validateProductOptions(attributes, variants) {
  const errors = {};
  const codes = new Set();
  attributes.forEach((attribute, index) => {
    const path = `attributes[${index}]`;
    const code = attribute.code.trim();
    if (!code || code.length > 100) errors[`${path}.code`] = 'Enter an option code (up to 100 characters)';
    else if (codes.has(code.toLowerCase())) errors[`${path}.code`] = 'Option codes must be unique';
    codes.add(code.toLowerCase());
    if (!attribute.name.trim() || attribute.name.trim().length > 100) {
      errors[`${path}.name`] = 'Enter an option name (up to 100 characters)';
    }
    if (attribute.description.length > 500) errors[`${path}.description`] = 'Use up to 500 characters';
    if (attribute.icon && (!attribute.icon.type.startsWith('image/') || attribute.icon.size > 5 * 1024 * 1024)) {
      errors[`${path}.icon`] = 'Choose an image up to 5MB';
    }
    if (!attribute.values.length) errors[`${path}.values`] = 'Add at least one value';
    const values = new Set();
    attribute.values.forEach((item, valueIndex) => {
      const value = item.value.trim();
      if (!value || value.length > 255) {
        errors[`${path}.values[${valueIndex}].value`] = 'Enter a value (up to 255 characters)';
      } else if (values.has(value.toLowerCase())) {
        errors[`${path}.values[${valueIndex}].value`] = 'Values within an option must be unique';
      }
      values.add(value.toLowerCase());
    });
  });

  variants.forEach((variant, index) => {
    const selectedIds = variant.attributeValueIds || [];
    const complete = selectedIds.length === attributes.length && attributes.every(attribute =>
      attribute.values.filter(value => selectedIds.includes(value.tempId)).length === 1);
    if (!complete) errors[`variant_${index}_attributeValueIds`] = 'Select one value for each option';
  });
  return errors;
}
