/* @flow */

import React, { PropTypes } from 'react';
import formatCurrency from 'lib/format-currency';
import type { HTMLElement } from 'types';

type CurrencyProps = {
  value: any;
  fractionBase: number;
  currency: string;
  bigNumber: bool;
  className?: string;
}

const Currency = (props: CurrencyProps): HTMLElement => {
  const {value, className, ...rest} = props;
  return <span className={className}>{formatCurrency(value, {...rest})}</span>;
};

Currency.propTypes = {
  value: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
  fractionBase: PropTypes.number,
  currency: PropTypes.string,
  bigNumber: PropTypes.bool,
};

Currency.defaultProps = {
  fractionBase: 2,
  currency: 'USD',
  bigNumber: false,
};

export default Currency;
