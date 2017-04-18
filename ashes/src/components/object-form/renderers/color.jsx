/* @flow */

// libs
import noop from 'lodash/noop';
import React from 'react';

// components
import renderFormField from '../form-field';
import SwatchInput from 'components/forms/swatch-input';

import type { FieldErrors, ChangeHandler } from './index';

export default function renderColor(errors: FieldErrors = {}, onChange: ChangeHandler = noop) {
  return function(name: string, value: any, options: AttrOptions) {
    const handler = v => onChange(name, 'color', v);
    const colorSwatch = (
      <SwatchInput
        value={value}
        onChange={handler}
      />
    );

    return renderFormField(name, colorSwatch, options);
  };
}
