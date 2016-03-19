//libs
import React, { PropTypes } from 'react';

//components
import DatePicker from '../../datepicker/datepicker';


const Input = ({value, changeValue}) => {
  return <DatePicker date={new Date(value)} onClick={(value) => changeValue(value.toString())}/>;
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
