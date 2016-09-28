import { get, castArray, noop } from 'lodash';
import { flow } from 'lodash/fp';
import cx from 'classnames';
import React from 'react';
import MultiSelect from 'react-widgets/lib/Multiselect';
import MaskInput from 'react-input-mask';
import { Field } from 'redux-form';

import { applyIfSet, popValue } from '../../utils/fp';
import messages from '../../core/lib/messages.json';

import styles from './fields.css';

import type { FormField as TFormField } from '../../core/types/fields';

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

const renderInput = ({ input, type, mask, maskChar = ' ', placeholder, meta }) => (
  <FormField input={input} meta={meta}>
    <MaskInput {...input} mask={mask} maskChar={maskChar} type={type} placeholder={placeholder} />
  </FormField>
);

const renderTextarea = ({ input, placeholder, meta }) => (
  <FormField input={input} meta={meta}>
    <textarea {...input} placeholder={placeholder} rows="1" />
  </FormField>
);

const renderSelect = ({ input, values, placeholder, meta, multi = true }) => {
  const value = applyIfSet(castArray)(input.value);
  const onChange = flow(popValue(multi), input.onChange);

  return (
    <FormField input={input} meta={meta}>
      <MultiSelect
        {...input}
        value={value}
        onChange={onChange}
        onBlur={noop}
        data={values}
        placeholder={placeholder}
      />
    </FormField>
  );
};

export default (field: TFormField) => {
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
