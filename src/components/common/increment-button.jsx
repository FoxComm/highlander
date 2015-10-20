'use strict';

import React from 'react';

const IncrementButton = (props) => {
  return (
    <button onClick={props.onClick}>
      <i className='icon-chevron-up'></i>
    </button>
  );
};

export default IncrementButton;