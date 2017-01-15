import React, { PropTypes } from 'react';
import classNames from 'classnames';
import formatCurrency from '../../lib/format-currency';

const Currency = (props) => {
  const {isTransaction, id, ...rest} = props;
  const className = classNames('fc-currency', {
    '_transaction': isTransaction,
    '_negative': parseInt(props.value, 10) < 0
  });

  return (
    <span id={id} className={ classNames(className, props.className) }>
      {formatCurrency(props.value, {...rest})}
    </span>
  );
};

Currency.propTypes = {
  value: PropTypes.oneOfType([PropTypes.number, PropTypes.string]).isRequired,
  fractionBase: PropTypes.number,
  currency: PropTypes.string,
  bigNumber: PropTypes.bool,
  isTransaction: PropTypes.bool,
};

Currency.defaultProps = {
  fractionBase: 2,
  currency: 'USD',
  bigNumber: false
};

export default Currency;
