'use strict';

import React from 'react';

const DecrementButton = (props) => {
  return (
    <button onClick={props.onClick}>
      <i className='icon-chevron-down'></i>
    </button>
  );
};

const DeleteButton = (props) => {
  return (
    <button className='fc-btn' onClick={props.onClick}>
      <i className='icon-trash'></i>
    </button>
  );
};

const EditButton = (props) => {
  return (
    <button className='fc-btn' onClick={props.onClick}>
      <i className='icon-edit'></i>
    </button>
  );
};

const IncrementButton = (props) => {
  return (
    <button onClick={props.onClick}>
      <i className='icon-chevron-up'></i>
    </button>
  );
};

const PrimaryButton = (props) => {
  return (
    <button className='fc-btn fc-btn-primary' onClick={props.onClick}>
      {props.children}
    </button>
  );
};

export {
  DecrementButton,
  DeleteButton,
  EditButton,
  IncrementButton,
  PrimaryButton
};
