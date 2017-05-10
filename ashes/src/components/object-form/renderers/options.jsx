/* @flow */

// libs
import { noop, get } from 'lodash/noop';
import React from 'react';

// components
import { Dropdown } from 'components/dropdown';
import renderFormField from '../form-field';

// types
import type { FieldErrors, ChangeHandler } from './index';

export default function renderOptions(errors: FieldErrors = {}, onChange: ChangeHandler = noop) {
  return function (name: string, value: any, options: AttrOptions) {
    const fieldOptions = this.props.fieldsOptions && this.props.fieldsOptions[name];
    if (!fieldOptions) throw new Error('You must define fieldOptions for options fields');

    const handler = v => onChange(name, 'options', v);
    options.error = get(errors, name);

    const dropdown = (
      <Dropdown
        value={value}
        items={fieldOptions}
        onChange={handler}
      />
    );

    return renderFormField(name, dropdown, options);
  };
}
