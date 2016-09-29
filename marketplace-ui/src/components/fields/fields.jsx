import { get, isEmpty, noop } from 'lodash';
import cx from 'classnames';
import React from 'react';
import MultiSelect from 'react-widgets/lib/Multiselect';
import MaskInput from 'react-input-mask';
import { Field } from 'redux-form';

import messages from '../../core/lib/messages.json';

import styles from './fields.css';

import type { FormField as TFormField } from '../../core/types/fields';

const setValue = (value, def = null) => !isEmpty(value) ? value : def;

/**
 * Form field wrapper
 */
const FormField = ({ input: { name }, meta: { error, touched }, children }) => {
  const hasError = touched && error;

  return (
    <div className={cx(styles.field, name, { [styles.fieldError]: hasError })}>
      {children}
      <span className={cx(styles.error, { [styles.errorActive]: hasError })}>
        {get(messages, error, 'Wrong value')}
      </span>
    </div>
  );
};

/**
 * Text input field
 */
const renderInput = ({ input, type, mask, maskChar = ' ', placeholder, meta }) => (
  <FormField input={input} meta={meta}>
    <MaskInput {...input} mask={mask} maskChar={maskChar} type={type} placeholder={placeholder} />
  </FormField>
);

/**
 * Textarea field
 */
const renderTextarea = ({ input, placeholder, meta }) => (
  <FormField input={input} meta={meta}>
    <textarea {...input} placeholder={placeholder} rows="1" />
  </FormField>
);

/**
 * Native select field
 */
const renderOptions = value => <option value={value} key={value}>{value}</option>;

const renderSelect = ({ input, values, placeholder, meta }) => (
  <FormField input={input} meta={meta}>
    {!input.value && <label htmlFor={input.name}>{placeholder}</label>}
    <select {...input} value={setValue(input.value, '')}>
      <option disabled />
      {values.map(renderOptions)}
    </select>
  </FormField>
);

/**
 * Multi-select "tags" field
 */
const renderTags = ({ input, values, placeholder, meta }) => (
  <FormField input={input} meta={meta}>
    <MultiSelect
      {...input}
      value={setValue(input.value)}
      onBlur={noop}
      data={values}
      placeholder={placeholder}
    />
  </FormField>
);

export default (field: TFormField) => {
  let renderField = renderInput;

  switch (field.type) {
    case 'select':
      renderField = renderSelect;
      break;
    case 'tags':
      renderField = renderTags;
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
