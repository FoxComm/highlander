/* @flow */

// libs
import React from 'react';

// components
import Counter from 'components/core/counter';

// types
import type { Context, ItemDesc } from '../types';

type Props = ItemDesc & {
  context: Context,
};

const CounterWidget = (props: Props) => {
  const value = props.context.params[props.name];
  const setValue = value => {
    props.context.setParams({
      [props.name]: value,
    });
  };

  return <Counter value={value} onChange={setValue} />;
};

export default CounterWidget;
