
import React from 'react';
import styles from '../attrs-edit.css';

import AppendInput from '../../forms/append-input';

const Percent = props => {
  const value = props.context.params[props.name];
  const onChange = (event) => {
    let percent = Number(event.target.value);
    percent = isNaN(percent) ? 0 : percent;
    percent = Math.min(100, percent);
    percent = Math.max(0, percent);

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

export default Percent;
