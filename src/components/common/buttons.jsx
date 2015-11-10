import React, { PropTypes } from 'react';
import classNames from 'classnames';
import _ from 'lodash';

const DefaultButton = (props = {}) => {
  const {icon, children, ...restProps} = props;

  return (
    <button {...restProps} className={ classNames('fc-btn', props.className) }>
      {icon && <i className={`icon-${icon}`}></i>}
      {children}
    </button>
  );
};

const LeftButton = props => {
  return <DefaultButton icon='chevron-left' {...props} />;
};

const RightButton = props => {
  return <DefaultButton icon='chevron-right' {...props} />;
};

const DecrementButton = props => {
  return <DefaultButton icon='chevron-down' {...props} />;
};

const DeleteButton = (props = {}) => {
  return <DefaultButton icon='trash' {...props} className={ classNames('fc-btn-remove', props.className) } />;
};

const EditButton = props => {
  return <DefaultButton icon='edit' {...props} />;
};

const IncrementButton = props => {
  return <DefaultButton icon='chevron-up' {...props} />;
};

const PrimaryButton = (props = {}) => {
  return (
    <DefaultButton {...props} className={ classNames('fc-btn-primary', props.className) }>
      {props.children}
    </DefaultButton>
  );
};

PrimaryButton.propTypes = {
  children: PropTypes.node
};

export {
  DefaultButton,
  LeftButton,
  RightButton,
  DecrementButton,
  DeleteButton,
  EditButton,
  IncrementButton,
  PrimaryButton
};
