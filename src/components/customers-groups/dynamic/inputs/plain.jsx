//libs
import React, { PropTypes } from 'react';

//components
import propTypes from '../widgets/propTypes';


export const Input = type => ({value, prefixed, changeValue}) => {
  return (
    <input className={prefixed('value')}
           type={type}
           onChange={({target}) => changeValue(target.value)}
           value={value} />
  );
};
Input.propTypes = propTypes;
