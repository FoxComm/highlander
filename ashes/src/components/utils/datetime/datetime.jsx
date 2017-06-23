/* @flow */

// libs
import React from 'react';
import moment from 'moment';

type DateTimeProps = {
  /** time string */
  value: string,
  /** set UTC time*/
  utc?: boolean,
  /** set empty value text */
  emptyValue?: string,
  /** className */
  className?: string
}

type MomentProps = DateTimeProps & {
  /** set time format */
  format: string,
};

/**
 * `DateTime`, `Date`, and `Time` - are simple components
 * build on the top of generic `Moment`
 * and serve to show date/time data
 *
 * @function DateTime
 */
export const DateTime = (props: DateTimeProps) => <Moment {...props} format={'L LT'} />;
export const Date = (props: DateTimeProps) => <Moment {...props} format={'L'} />;
export const Time = (props: DateTimeProps) => <Moment {...props} format={'LT'} />;

const Moment = ({
  utc,
  value,
  format,
  emptyValue,
  className,
  ...rest
}: MomentProps) => {

  if (!value) {
    return <span className={className}>{emptyValue}</span>;
  }

  const timeValue = utc ? moment.utc(value) : moment(value);

  return (
    <time className={className} dateTime={timeValue.local().format()} {...rest}>
      {timeValue.local().format(format)}
    </time>
  );
};

Moment.defaultProps = {
  format: 'L LTS',
  utc: true,
  emptyValue: 'not set',
};
