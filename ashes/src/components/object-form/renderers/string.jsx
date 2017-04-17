import noop from 'lodash/noop';
import React from 'react';

import { renderFormField } from '../object-form-inner';

const inputClass = 'fc-object-form__field-value';

export default function renderString(state: Object, onChange: Function = noop) {
  return function (name: string, value: string = '', options: AttrOptions) {
    const handler = ({ target }) => {
      return onChange(name, 'string', target.value);
    };

    const stringInput = (
      <input
        className={inputClass}
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
