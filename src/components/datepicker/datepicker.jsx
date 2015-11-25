import React, { PropTypes } from 'react';

const DatePicker = props => {
  return (
    <div className='fc-form-field'>
      <input type='date' {...props}/>
    </div>
  );
};


export default DatePicker;


