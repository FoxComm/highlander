'use strict';

import React from 'react';
import _ from 'lodash';

const DefaultButton = (props) => {
  const buttonProps = _.omit(props, 'icon', 'children', 'className');
  return (
    <button className={`fc-btn ${props.className}`} {...buttonProps}>
      {props.icon &&
        <i className={`icon-${props.icon}`}></i>
      }
      {props.children}
    </button>
  );
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

const IncrementButton = (props) => {
  return <DefaultButton icon='chevron-up' {...props} />
};

const PrimaryButton = (props) => {
  return (
    <DefaultButton className='fc-btn-primary'>
      {props.children}
    </DefaultButton>
  );
};

export {
  DefaultButton,
  DecrementButton,
  DeleteButton,
  EditButton,
  IncrementButton,
  PrimaryButton
};
