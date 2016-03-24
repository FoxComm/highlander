//libs
import React, { PropTypes } from 'react';

//helpers
import formatCurrency, { stringToCurrency } from '../../../../lib/format-currency';

//components
import CurrencyInput from '../../../forms/currency-input';


const propTypes = {
  criterion: PropTypes.shape({
    field: PropTypes.string.isRequired,
  }).isRequired,
  prefixed: PropTypes.func.isRequired,
  value: PropTypes.any,
  changeValue: PropTypes.func,
};

const Input = ({value, changeValue}) => {
  return (
    <CurrencyInput onChange={changeValue}
                   value={value} />
  );
};
Input.propTypes = propTypes;

const Label = ({value, prefixed}) => {
  return (
    <div className={prefixed('value')}>
      ${formatCurrency(value, {bigNumber: true, fractionBase: 2})}
    </div>
  );
};
Label.propTypes = propTypes;

export default {
  Input,
  Label,
};
