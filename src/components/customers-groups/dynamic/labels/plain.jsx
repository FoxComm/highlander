//libs
import React, { PropTypes } from 'react';

//components
import propTypes from '../widgets/propTypes';


export const Label = type => ({value, prefixed}) => {
  return (
    <div className={prefixed('value')}>
      {value}
    </div>
  );
};
Label.propTypes = propTypes;
