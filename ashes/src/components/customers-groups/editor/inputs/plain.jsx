//libs
import React from 'react';

//helpers
import { prefix } from 'lib/text-utils';

//components
import propTypes from '../widgets/propTypes';

export const Input = type => ({value, className, changeValue}) => {
  const prefixed = prefix(className);

  return (
    <input className={prefixed('value')}
           type={type}
           onChange={({target}) => changeValue(target.value)}
           value={value} />
  );
};
Input.propTypes = propTypes;

export const getDefault = type => () => {
  switch (type) {
    case 'number':
      return 0;
    default:
      return '';
  }
};

export const isValid = type => value => {
  switch (type) {
    case 'number':
      return true;
    default:
      return Boolean(value);
  }
};
