/* @flow */

// libs
import React, { Element } from 'react';
import classNames from 'classnames';

// styles
import s from '../attrs-edit.css';

// components
import Counter from '../../forms/counter';

// types
import type { Context, ItemDesc } from '../types';

type Props = ItemDesc & {
  context: Context;
};

function toNumber(value, def = 1) {
  const asNumber = Number(value);
  return isNaN(asNumber) ? def : asNumber;
}

const CounterWidget = (props: Props): Element<*> => {
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
      className={classNames(s['inline-edit-input'], s.counterInput)}
      value={value}
      onChange={event => setValue(event.target.value)}
      {...actions}
    />
  );
};

export default CounterWidget;
