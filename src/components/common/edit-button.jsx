'use strict';

import React from 'react';

const EditButton = (props) => {
  return (
    <button className='fc-btn' onClick={props.onClick}>
      <i className='icon-edit'></i>
    </button>
  );
};

export default EditButton;