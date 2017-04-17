/* @flow */

// libs
import noop from 'lodash/noop';
import React from 'react';

// components
import renderFormField from './form-field';

export default function renderString(state: Object, onChange: Function = noop) {
  return function (name: string, value: string = '', options: AttrOptions) {
    const handler = ({ target }) => {
      return onChange(name, 'string', target.value);
    };

    const stringInput = (
      <input
        className='fc-object-form__field-value'
        type="text"
        name={name}
        value={value || ''}
        onChange={handler}
        disabled={options.disabled}
      />
    );

    return renderFormField(name, stringInput, options);
  };
}
