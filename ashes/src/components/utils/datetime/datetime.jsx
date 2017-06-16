/* @flow */

// libs
import React from 'react';
import moment from 'moment';
import classnames from 'classnames';

// styles
import s from './datetime.css';

type Props = {
  /** time string */
  value?: string,
  /** set UTC  */
  utc?: boolean,
  /** set time format */
  format?: string,
  /** set empty value text */
  emptyValue?: string,
  /** additional className */
  className?: string
}

/**
 * `DateTime`, `Date`, and `Time` - are simple components
 * build on the top of `Moment`
 * and serve to show time data
 *
 * @function Moment
 */
export const Moment = ({
  utc,
  value,
  format,
  emptyValue,
  className,
  ...rest
}: Props) => {
  const cls = classnames(s.time, className);

  if (!value) {
    return <span className={cls}>{emptyValue}</span>;
  }

  const timeValue = utc ? moment.utc(value) : moment(value);

  return (
    <time className={cls} dateTime={timeValue.local().format()} {...rest}>
      {timeValue.local().format(format)}
    </time>
  );
};

Moment.defaultProps = {
  format: 'L LTS',
  utc: true,
  emptyValue: 'not set',
};


export const DateTime = (props: Props) => <Moment {...props} format={'L LT'} />;
export const Date = (props: Props) => <Moment {...props} format={'L'} />;
export const Time = (props: Props) => <Moment {...props} format={'LT'} />;
