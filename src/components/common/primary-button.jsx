'use strict';

import React from 'react';

const PrimaryButton = (props) => {
  return (
    <button className='fc-btn fc-btn-primary' onClick={props.onClick}>
      {props.children}
    </button>
  );
};

export default PrimaryButton;