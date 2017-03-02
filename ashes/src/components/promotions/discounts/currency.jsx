/* @flow */

import React, { Element } from 'react';
import styles from '../attrs-edit.css';

import CurrencyInput from '../../forms/currency-input';

const Currency = (props) => {
  return (
    <CurrencyInput
      styleName="inline-edit-input"
      value={props.value}
      onChange={props.onChange}
    />
  );
};

export default Currency;
