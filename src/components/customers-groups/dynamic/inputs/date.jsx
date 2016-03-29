//libs
import moment from 'moment';
import React, { PropTypes } from 'react';

//components
import DatePicker from '../../../datepicker/datepicker';
import propTypes from '../widgets/propTypes';
import storedDateFormat from '../widgets/date';

export const Input = ({value, changeValue}) => {
  return (
    <DatePicker date={new Date(value)}
                onClick={(value) => changeValue(moment(value).format(storedDateFormat))} />
  );
};
Input.propTypes = propTypes;
