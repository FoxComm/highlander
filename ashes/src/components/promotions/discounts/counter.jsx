/* @flow */

import React, { Element } from 'react';
import styles from './counter.css';

import Counter from '../../forms/counter';


function toNumber(value, def = 1) {
  const asNumber = Number(value);
  return isNaN(asNumber) ? def : asNumber;
}

const CounterWidget = (props) => {
  const value = toNumber(props.value);
  const setValue = value => {
    props.onChange(value);
  };

  const actions = {
    increaseAction() {
      setValue(toNumber(value) + 1);
    },
    decreaseAction() {
      setValue(Math.max(1, toNumber(value) - 1));
    }
  };

  return (
    <Counter
      styleName="counter-input"
      value={value}
      onChange={event => setValue(event.target.value)}
      {...actions}
    />
  );
};

export default CounterWidget;
