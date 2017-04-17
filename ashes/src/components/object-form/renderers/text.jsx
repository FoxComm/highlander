/* @flow */

// libs
import noop from 'lodash/noop';
import React from 'react';

// components
import renderFormField from './form-field';

import type { FieldErrors, ChangeHandler } from './index';

export default function renderText(errors: FieldErrors = {}, onChange: ChangeHandler = noop) {
  return function(name: string, value: string = '', options: AttrOptions) {
    const handler = ({ target }) => {
      return onChange(name, 'text', target.value);
    };
    const textInput = (
      <textarea
        className='fc-object-form__field-value'
        name={name}
        onChange={handler}
        value={value}
      />
    );

    return renderFormField(name, textInput, options);
  };
}
