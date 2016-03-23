//libs
import React, { PropTypes } from 'react';

//components
import CurrencyInput from '../../../forms/currency-input';


const Input = ({value, changeValue}) => {
  return (
    <CurrencyInput onChange={changeValue}
                   value={value} />
  );
};

Input.propTypes = {
  criterion: PropTypes.shape({
    field: PropTypes.string.isRequired,
  }).isRequired,
  prefixed: PropTypes.func.isRequired,
  value: PropTypes.any,
  changeValue: PropTypes.func.isRequired,
};

export default Input;
