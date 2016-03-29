//libs
import moment from 'moment';
import React, { PropTypes } from 'react';

//components
import propTypes from '../widgets/propTypes';
import labelDateFormat from '../widgets/date';


export const Label = ({value, prefixed}) => {
  return (
    <div className={prefixed('value')}>
      {moment(value).format(labelDateFormat)}
    </div>
  );
};
Label.propTypes = propTypes;
