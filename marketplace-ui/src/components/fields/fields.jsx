import cx from 'classnames';
import React from 'react';
import { Field } from 'redux-form';

import styles from './fields.css';

import type { FormField } from '../../core/types/fields';

const renderInput = ({ input, type, placeholder, meta }) => {
  const hasError = meta.touched && meta.error;

  return (
    <div className={cx(styles.field, { [styles.fieldError]: hasError })}>
      <input {...input} placeholder={placeholder} type={type} />
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

  return (
    <div className={cx(styles.field, { [styles.fieldError]: hasError })}>
      {!input.value && <label htmlFor={input.name}>{placeholder}</label>}
      <select {...input}>
        <option disabled />
        {values.map(value =>
          <option value={value} key={value}>{value}</option>
        )}
      </select>
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
