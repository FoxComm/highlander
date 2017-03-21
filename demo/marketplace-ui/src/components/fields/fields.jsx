import { get, isEmpty, invoke, noop, upperFirst } from 'lodash';
import cx from 'classnames';
import React from 'react';
import MultiSelect from 'react-widgets/lib/Multiselect';
import MaskInput from 'react-input-mask';
import { Field, Fields } from 'redux-form';

import messages from '../../core/lib/messages.json';

import styles from './fields.css';

import type { FormField as TFormField } from '../../core/types/fields';

const setValue = (value, def = null) => (!isEmpty(value) ? value : def);

/**
 * Form field wrapper
 */
export const FormField = ({ input: { name }, type, meta: { error, touched }, children }) => {
  const hasError = touched && error;

  return (
    <div className={cx(styles.field, name, styles[`field${upperFirst(type)}`], { [styles.fieldError]: hasError })}>
      {children}
      <span className={cx(styles.error, { [styles.errorActive]: hasError })}>
        {hasError ? get(messages, error, 'Wrong value') : ''}
      </span>
    </div>
  );
};

/**
 * Text input field
 */
const renderInput = ({ input, type, mask, maskChar = ' ', placeholder, meta }) => (
  <FormField input={input} type={type} meta={meta}>
    <MaskInput {...input} mask={mask} maskChar={maskChar} type={type} placeholder={placeholder} />
  </FormField>
);

/**
 * Textarea field
 */
const renderTextarea = ({ input, type, placeholder, meta }) => (
  <FormField input={input} type={type} meta={meta}>
    <textarea {...input} placeholder={placeholder} rows="1" />
  </FormField>
);

/**
 * Radio field
 */
const renderRadio = ({ input, meta }) => (
  <FormField input={input} meta={meta}>
    <label htmlFor={input.value}><input {...input} type="radio" id={input.value} />{input.value}</label>
  </FormField>
);

const renderRadios = (field: TFormField) => (
  <div className={cx(styles.radioGroup, field.name)} key={field.name}>
    <span className={styles.placeholder}>{field.placeholder}</span>
    <div>
      {field.values.map(value => (
        <Field {...field} value={value} component={renderRadio} key={`${field.name}-${value}`} />
      ))}
    </div>
  </div>
);

/**
 * Checkbox field
 */
const renderCheckbox = ({ input, type, placeholder, meta }) => (
  <FormField input={input} type={type} meta={meta}>
    <label htmlFor={input.name}><input {...input} type="checkbox" id={input.name} />{placeholder}</label>
  </FormField>
);

/**
 * Native select field
 */
const renderOptions = value => <option key={value}>{value}</option>;

const renderSelect = ({ input, values, type, placeholder, meta }) => (
  <FormField input={input} type={type} meta={meta}>
    {!input.value && <span className={styles.placeholderInline}>{placeholder}</span>}
    <select {...input} value={setValue(input.value, '')}>
      {!input.value && <option disabled />}
      {values.map(renderOptions)}
    </select>
  </FormField>
);

/**
 * Multi-select "tags" field
 */
const renderTags = ({ input, values, placeholder, meta }) => (
  <FormField input={input} className={input.name} meta={meta}>
    <MultiSelect
      {...input}
      value={setValue(input.value)}
      onBlur={noop}
      data={values}
      placeholder={placeholder}
    />
  </FormField>
);

const renderFile = ({ input, file, type, placeholder, meta }) => {
  const fileName = get(file, '[0].name');

  return (
    <FormField input={input} type={type} meta={meta}>
      <span className={styles.placeholder}>{placeholder}</span>
      <div className={styles.file}>
        <span>Select File</span>
        <input {...input} onBlur={noop} type="file" />
      </div>
      {fileName && <span className={styles.fileName}>Selected file: {fileName}</span>}
    </FormField>
  );
};

const typeRendererMap = {
  select: renderSelect,
  checkbox: renderCheckbox,
  tags: renderTags,
  textarea: renderTextarea,
  file: renderFile,
};

export default (field: TFormField, values: string) => {
  if (field.showPredicate && !invoke(field, 'showPredicate', values)) {
    return;
  }

  // TODO: better implementation of radio rendering.
  // The problem is that it requires one Field component for each radio, not a radio group
  if (field.type === 'radio') {
    return renderRadios(field);
  }

  const renderField = get(typeRendererMap, field.type, renderInput);

  return (
    <Field
      {...field}
      component={renderField}
      key={field.name}
    />
  );
};
