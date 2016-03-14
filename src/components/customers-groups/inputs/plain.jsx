//libs
import React, { PropTypes } from 'react';


const Input = ({criterion, value, prefixed, changeValue}) => {
  return (
    <input className={prefixed('field')}
           name={criterion.field}
           maxLength='255'
           type='text'
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
