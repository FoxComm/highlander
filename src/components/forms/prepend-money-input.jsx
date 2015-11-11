import React from 'react';

const PrependMoneyInput = props => {
  return (
    <div className='fc-input-group'>
      <div className='fc-input-prepend'><i className='icon-usd'></i></div>
      <input type='text' {...props} />
    </div>
  );
};

export default PrependMoneyInput;
