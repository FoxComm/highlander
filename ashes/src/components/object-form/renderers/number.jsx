/* @flow */

// libs
import noop from 'lodash/noop';
import React from 'react';

// components
import renderFormField from '../form-field';

import type { FieldErrors, ChangeHandler } from './index';

export default function renderNumber(errors: FieldErrors = {}, onChange: ChangeHandler = noop) {
  return function(name: string, value: ?number = null, options: AttrOptions) {
    const handler = ({ target }) => {
      return onChange(name, 'number', target.value == '' ? null : Number(target.value));
    };
    const stringInput = (
      <input
        className='fc-object-form__field-value'
        type="number"
        name={name}
        value={value == null ? '' : value}
        onChange={handler}
      />
    );

    return renderFormField(name, stringInput, options);
  };
}