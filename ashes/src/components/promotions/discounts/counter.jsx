/* @flow */

// libs
import React, { Element } from 'react';

// components
import Counter from 'components/core/counter';

// types
import type { Context, ItemDesc } from '../types';

type Props = ItemDesc & {
  context: Context,
};

const CounterWidget = (props: Props): Element<*> => {
  const value = props.value;
  const setValue = value => {
    props.onChange(value);
  };

  return <Counter value={value} onChange={setValue} min={1} />;
};

export default CounterWidget;
