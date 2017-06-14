/* @flow */

// libs
import classNames from 'classnames';
import React from 'react';

// styles
import s from './change.css';

type Props = {
  /** value of change */
  value: number
}

/**
 * Change component serves to render colored number changes
 *
 * @function Change
 */
const Change = (props: Props) => {
  const { value } = props;

  const cls = classNames(s.change, {
    [s.positive]: value > 0,
    [s.negative]: value < 0
  });

  return <span className={cls}>{Math.abs(value)}</span>;
};

export default Change;
