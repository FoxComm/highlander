//libs
import React, { PropTypes } from 'react';

//components
import CurrencyInput from '../../../forms/currency-input';
import propTypes from '../widgets/propTypes';


export const Input = ({value, changeValue}) => {
  return (
    <CurrencyInput onChange={changeValue}
                   value={value} />
  );
};
Input.propTypes = propTypes;
