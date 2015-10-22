'use strict';

import React from 'react';

const PrimaryButton = (props) => {
  return (
    <button className='fc-btn fc-btn-primary' {...props}>
      {props.children}
    </button>
  );
};

export default PrimaryButton;
