//libs
import _ from 'lodash';
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

export const getDefault = () => 0;

export const isValid = value => true;
