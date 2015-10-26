'use strict';

import React from 'react';

const DecrementButton = (props) => {
  return (
    <button {...props}>
      <i className='icon-chevron-down'></i>
    </button>
  );
};

export default DecrementButton;
