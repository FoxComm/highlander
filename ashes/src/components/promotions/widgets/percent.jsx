/* @flow */
import React, { Element } from 'react';
import styles from '../attrs-edit.css';

import AppendInput from '../../forms/append-input';

import type { Context, ItemDesc } from '../types';

type Props = ItemDesc & {
  context: Context,
};

const Percent = (props: Props) => {
  const value = props.context.params[props.name] || 0;
  const onChange = (event: Object) => {
    const percent = Percent.getValue(event.target.value);

    props.context.setParams({
      [props.name]: percent
    });
  };

  return (
    <AppendInput
      styleName="inline-edit-input"
      min={0}
      max={100}
      type="number"
      plate="%"
      value={value}
      onChange={onChange}
    />
  );
};

Percent.getValue = function(value) {
  let percent = Number(value);
  percent = isNaN(percent) ? 0 : percent;
  percent = Math.min(100, percent);
  percent = Math.max(0, percent);
  return percent;
};

export default Percent;
