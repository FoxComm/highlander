//libs
import React, { PropTypes } from 'react';

//components
import CurrencyInput from '../../forms/currency-input';


const Input = ({criterion, value, prefixed, changeValue}) => {
  return (
    <CurrencyInput inputName={criterion.field}
                   onChange={changeValue}
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
