//libs
import React, { PropTypes } from 'react';


const Input = type => ({criterion, value, prefixed, changeValue}) => {
  return (
    <input className={prefixed('field')}
           name={criterion.field}
           type={type}
           onChange={({target}) => changeValue(target.value)}
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
