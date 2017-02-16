/* @flow */

import React, { Element } from 'react';
import styles from '../attrs-edit.css';

import CurrencyInput from '../../forms/currency-input';

import type { Context, ItemDesc } from '../types';

type Props = ItemDesc & {
  context: Context;
}

const Currency = (props: Props) => {
  const value = props.context.params[props.name];
  const onChange = value => {
    props.context.setParams({
      [props.name]: Number(value)
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

export default Currency;
