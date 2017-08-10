/* @flow */

// libs
import { identity } from 'lodash';
import classNames from 'classnames';
import React from 'react';

// styles
import s from './change.css';

type Props = {
  /** value of change */
  value: number,
  /** value formatting function */
  format?: (value: number) => string
};

/**
 * Change component serves to render colored number changes
 *
 * @function Change
 */
const Change = ({ value, format = identity }: Props) => {

  const cls = classNames(s.change, {
    [s.positive]: value > 0,
    [s.negative]: value < 0,
  });

  return <span className={cls}>{format(Math.abs(value))}</span>;
};

export default Change;
