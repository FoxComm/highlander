/* @flow */

// libs
import noop from 'lodash/noop';
import React from 'react';

// components
import renderFormField from './form-field';
import { SliderCheckbox } from 'components/checkbox/checkbox';

export default function renderBoolean(state: Object, onChange: Function = noop) {
  return function (name: string, value: boolean, options: AttrOptions) {
    const handler = () => onChange(name, 'bool', !value);

    const sliderCheckbox = (
      <SliderCheckbox
        id={name}
        checked={value}
        onChange={handler}
      />
    );

    return renderFormField(name, sliderCheckbox, options);
  };
}


