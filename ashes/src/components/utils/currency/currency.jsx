/* @flow */

// libs
import { curryRight } from 'lodash';
import React from 'react';
import classNames from 'classnames';

// components
import formatCurrency from '../../../lib/format-currency';
import Change from 'components/utils/change';

// styles
import s from './currency.css';

type Props = {
  /** element's id */
  id: number,
  /** passing value */
  value: number | string,
  /** fraction base */
  fractionBase: number,
  /** currency abbreviation (e.g. 'EUR') */
  currency: string,
  /** set true if value is big number */
  bigNumber: boolean,
  /** transaction mode renders colored positive/negative values */
  isTransaction?: boolean,
  /** additional className */
  className?: string
}

/**
 * Currency component serves to format passed value
 * and render it with currency symbol
 *
 * @function Change
 */

const Currency = (props: Props) => {
  const {isTransaction, id, className, ...rest} = props;
  let value;

  if (isTransaction) {
    value = <Change value={props.value} format={curryRight(formatCurrency)(rest)} />;
  } else {
    value = formatCurrency(props.value, rest);
  }

  return (
    <span id={id} className={classNames(s.currency, className)}>
      {value}
    </span>
  );
};

Currency.defaultProps = {
  fractionBase: 2,
  currency: 'USD',
  bigNumber: false
};

export default Currency;
