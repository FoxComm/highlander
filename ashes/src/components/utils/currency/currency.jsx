/* @flow */

// libs
import React from 'react';
import classNames from 'classnames';

// components
import formatCurrency from '../../../lib/format-currency';

// styles
import s from './currency.css';

type Props = {
  /** passing value */
  value: number | string,
  /** fraction base */
  fractionBase: number,
  /** currency name */
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
  const currencyCls = classNames(s.currency, {
    [s.transaction]: isTransaction,
    [s.negative]: parseInt(props.value, 10) < 0
  });

  return (
    <span id={id} className={classNames(currencyCls, className)}>
      {formatCurrency(props.value, {...rest})}
    </span>
  );
};

Currency.defaultProps = {
  fractionBase: 2,
  currency: 'USD',
  bigNumber: false
};

export default Currency;
