/* @flow */

import React, { Element } from 'react';
import styles from '../attrs-edit.css';

import CurrencyInput from '../../forms/currency-input';

import type { Context, ItemDesc } from '../types';

type Props = ItemDesc & {
  context: Context;
}

function toNumber(value, def = 0) {
  const asNumber = Number(value);
  return isNaN(asNumber) ? def : asNumber;
}


const Currency = (props: Props) => {
  const value = props.context.params[props.name];
  const onChange = value => {
    props.context.setParams({
      [props.name]: Currency.getValue(value)
    });
  };

  return (
    <CurrencyInput
      styleName="inline-edit-input"
      value={value}
      onChange={onChange}
    />
  );
};

Currency.getValue = function(value) {
  return toNumber(value);
};

export default Currency;
