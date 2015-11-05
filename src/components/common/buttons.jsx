'use strict';

import React from 'react';
import _ from 'lodash';

const DefaultButton = (props) => {
  const {icon, children, ...restProps} = props;
  const buttonProps = {
    ...restProps,
    className: `fc-btn ${props.className || ''}`.trim()
  };
  return (
    <button {...buttonProps}>
      {icon && <i className={`icon-${icon}`}></i>}
      {children}
    </button>
  );
};

const LeftButton = (props) => {
  return <DefaultButton icon='chevron-left' {...props} />;
};

const RightButton = (props) => {
  return <DefaultButton icon='chevron-right' {...props} />;
};

const DecrementButton = (props) => {
  return <DefaultButton icon='chevron-down' {...props} />;
};

const DeleteButton = (props) => {
  return <DefaultButton icon='trash' {...props} />;
};

const EditButton = (props) => {
  return <DefaultButton icon='edit' {...props} />;
};

const AddButton = (props) => {
  return <DefaultButton icon='add' {...props} />;
};

const IncrementButton = (props) => {
  return <DefaultButton icon='chevron-up' {...props} />;
};

const PrimaryButton = (props) => {
  const buttonProps = {
    ...props,
    className: `fc-btn-primary ${props.className || ''}`
  };

  return (
    <DefaultButton {...buttonProps}>
      {props.children}
    </DefaultButton>
  );
};

export {
  DefaultButton,
  LeftButton,
  RightButton,
  DecrementButton,
  DeleteButton,
  EditButton,
  AddButton,
  IncrementButton,
  PrimaryButton
};
