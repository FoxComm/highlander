/* @flow */
import React, { Element } from 'react';
import styles from '../attrs-edit.css';

import AppendInput from '../../forms/append-input';

const Percent = (props) => {
  const value = props.value || 0;
  const setValue = value => {
    let percent = Number(value);
    percent = isNaN(percent) ? 0 : percent;
    percent = Math.min(100, percent);
    percent = Math.max(0, percent);
    props.onChange(percent);
  };

  return (
    <AppendInput
      styleName="inline-edit-input"
      min={0}
      max={100}
      type="number"
      plate="%"
      value={value}
      onChange={event => setValue(event.target.value)}
    />
  );
};

export default Percent;
