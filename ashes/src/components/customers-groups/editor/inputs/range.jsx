//libs
import React from 'react';
import classNames from 'classnames';

//helpers
import { prefix } from 'lib/text-utils';

//components
import propTypes from '../widgets/propTypes';


export const Input = ({Input: Widget}) => ({criterion, value, className, changeValue}) => {
  const prefixed = prefix(prefix(className)('range'));
  const values = value || [null, null];

  const change = index=> (value) => {
    changeValue([
      ...values.slice(0, index),
      value,
      ...values.slice(index + 1),
    ]);
  };

  return (
    <div className={prefixed()}>
      {Widget({criterion, value: values[0], className, changeValue: change(0)})}
      <span className={classNames(prefixed('separator'), 'icon-minus', 'fc-align-center')} />
      {Widget({criterion, value: values[1], className, changeValue: change(1)})}
    </div>
  );
};
Input.propTypes = propTypes;

export const getDefault = Widget => criterion => [Widget.getDefault(criterion), Widget.getDefault(criterion)];

export const isValid = Widget => (value, criterion) => value.every(part => Widget.isValid(part, criterion));
