/* @flow */

// libs
import noop from 'lodash/noop';
import React from 'react';

// components
import renderFormField from './form-field';
import DatePicker from 'components/datepicker/datepicker';

export default function renderDate(state: Object, onChange: Function = noop) {
  return function (name: string, value: string, options: AttrOptions) {
    const dateValue = new Date(value);
    const handler = (v: Date) => onChange(name, 'date', v.toISOString());
    const dateInput = <DatePicker date={dateValue} onChange={handler} />;

    return renderFormField(name, dateInput, options);
  };
}


