'use strict';

import React from 'react';

const IncrementButton = (props) => {
  return (
    <button {...props}>
      <i className='icon-chevron-up'></i>
    </button>
  );
};

export default IncrementButton;
