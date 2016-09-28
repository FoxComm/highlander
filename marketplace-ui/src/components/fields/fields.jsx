import isEmpty from 'lodash/isEmpty';
import noop from 'lodash/noop';
import cx from 'classnames';
import React from 'react';
import MultiSelect from 'react-widgets/lib/Multiselect';
import MaskInput from 'react-input-mask';
import { Field } from 'redux-form';

import styles from './fields.css';

import type { FormField } from '../../core/types/fields';

const renderInput = ({ input, type, mask, maskChar, placeholder, meta }) => {
  const hasError = meta.touched && meta.error;
  const maskCharValue = maskChar || ' ';

  return (
    <div className={cx(styles.field, { [styles.fieldError]: hasError })}>
      <MaskInput {...input} mask={mask} maskChar={maskCharValue} type={type} placeholder={placeholder} />
      {<span className={cx(styles.error, { [styles.errorActive]: hasError })}>{meta.error}</span>}
    </div>
  );
};

const renderTextarea = ({ input, placeholder, meta }) => {
  const hasError = meta.touched && meta.error;

  return (
    <div className={cx(styles.field, { [styles.fieldError]: hasError })}>
      <textarea {...input} placeholder={placeholder} rows="1" />
      {<span className={cx(styles.error, { [styles.errorActive]: hasError })}>{meta.error}</span>}
    </div>
  );
};

const renderSelect = ({ input, values, placeholder, meta }) => {
  const hasError = meta.touched && meta.error;
  const value = !isEmpty(input.value) ? input.value : null;

  return (
    <div className={cx(styles.field, { [styles.fieldError]: hasError })}>
      {!value && <label htmlFor={input.name}>{placeholder}</label>}
      <MultiSelect {...input} value={value} onBlur={noop} data={values} />
      {<span className={cx(styles.error, { [styles.errorActive]: hasError })}>{meta.error}</span>}
    </div>
  );
};

export default (field: FormField) => {
  let renderField = renderInput;

  switch (field.type) {
    case 'select':
      renderField = renderSelect;
      break;
    case 'textarea':
      renderField = renderTextarea;
      break;
    case 'text':
    case 'number':
    default:
      renderField = renderInput;
  }

  return (
    <Field
      {...field}
      component={renderField}
      key={field.name}
    />
  );
};
