//libs
import moment from 'moment';
import React, { PropTypes } from 'react';

//components
import DatePicker from '../../../datepicker/datepicker';


const propTypes = {
  criterion: PropTypes.shape({
    field: PropTypes.string.isRequired,
  }).isRequired,
  prefixed: PropTypes.func.isRequired,
  value: PropTypes.any,
  changeValue: PropTypes.func.isRequired,
};

const Input = ({value, changeValue}) => {
  return <DatePicker date={new Date(value)}
                     onClick={(value) => changeValue(moment(value).format('YYYY-MM-DDTHH:mm:ss.SSSZ'))} />;
};
Input.propTypes = propTypes;

const Label = ({value, prefixed}) => {
  return (
    <div className={prefixed('')}>
      {moment(value).format('MM-DD-YYYY')}
    </div>
  );
};
Label.propTypes = propTypes;

export default {
  Input,
  Label,
};
