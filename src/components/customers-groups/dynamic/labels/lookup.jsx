//libs
import React, { PropTypes } from 'react';

//components
import propTypes from '../widgets/propTypes';


export const Label = ({value, prefixed}) => {
  return (
    <div className={prefixed('value')}>
      {value}
    </div>
  );
};
Label.propTypes = {
  ...propTypes,
  data: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.any,
    label: PropTypes.string,
  })),
};
