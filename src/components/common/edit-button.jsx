'use strict';

import React from 'react';

const EditButton = (props) => {
  return (
    <button className='fc-btn' {...props}>
      <i className='icon-edit'></i>
    </button>
  );
};

export default EditButton;
