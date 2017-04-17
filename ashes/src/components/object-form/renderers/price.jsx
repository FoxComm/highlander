/* @flow */

// libs
import React from 'react';
import { noop, get } from 'lodash';

// components
import renderFormField from './form-field';
import CurrencyInput from 'components/forms/currency-input';

export default function renderPrice(state: Object, onChange: Function = noop) {
  return function(name: string, value: any, options: AttrOptions) {
    const priceValue: string = get(value, 'value', '');
    const priceCurrency: string = get(value, 'currency', 'USD');
    const handler = value => onChange(name, 'price', {
      currency: priceCurrency,
      value: Number(value)
    });
    const currencyInput = (
      <CurrencyInput
        inputClass='fc-object-form__field-value'
        inputName={name}
        value={priceValue}
        onChange={handler}
      />
    );

    return renderFormField(name, currencyInput, options);
  };
}


