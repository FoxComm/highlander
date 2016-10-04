import { castArray, noop } from 'lodash';
import { flow } from 'lodash/fp';
import cx from 'classnames';
import React from 'react';
import MultiSelect from 'react-widgets/lib/Multiselect';
import MaskInput from 'react-input-mask';
import { Field } from 'redux-form';

import { applyIfSet, popValue } from '../../utils/fp';

import styles from './fields.css';

import type { FormField } from '../../core/types/fields';

const renderInput = ({ input, type, mask, maskChar = ' ', placeholder, meta }) => {
  const hasError = meta.touched && meta.error;

  return (
    <div className={cx(styles.field, input.name, { [styles.fieldError]: hasError })}>
      <MaskInput {...input} mask={mask} maskChar={maskChar} type={type} placeholder={placeholder} />
      {<span className={cx(styles.error, { [styles.errorActive]: hasError })}>{meta.error}</span>}
    </div>
  );
};

const renderTextarea = ({ input, placeholder, meta }) => {
  const hasError = meta.touched && meta.error;

  return (
    <div className={cx(styles.field, input.name, { [styles.fieldError]: hasError })}>
      <textarea {...input} placeholder={placeholder} rows="1" />
      {<span className={cx(styles.error, { [styles.errorActive]: hasError })}>{meta.error}</span>}
    </div>
  );
};

const renderSelect = ({ input, values, placeholder, meta, multi = true }) => {
  const hasError = meta.touched && meta.error;
  const value = applyIfSet(castArray)(input.value);
  const onChange = flow(popValue(multi), input.onChange);

  return (
    <div className={cx(styles.field, input.name, { [styles.fieldError]: hasError })}>
      <MultiSelect
        {...input}
        value={value}
        onChange={onChange}
        onBlur={noop}
        data={values}
        placeholder={placeholder}
      />
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
