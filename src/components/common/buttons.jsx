
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import _ from 'lodash';

const Button = (props = {}) => {
  const {icon, children, ...restProps} = props;

  return (
    <button {...restProps} className={ classNames('fc-btn', props.className) }>
      {icon && <i className={`icon-${icon}`}></i>}
      {children}
    </button>
  );
};

Button.propTypes = {
  className: PropTypes.string
};

const LeftButton = props => {
  return <Button icon='chevron-left' {...props} />;
};

const RightButton = props => {
  return <Button icon='chevron-right' {...props} />;
};

const DecrementButton = props => {
  return <Button icon='chevron-down' {...props} />;
};

const DeleteButton = (props = {}) => {
  return <Button icon='trash' {...props} className={ classNames('fc-btn-remove', props.className) } />;
};

const EditButton = props => {
  return <Button icon='edit' {...props} />;
};

const AddButton = props => {
  return <Button icon='add' {...props} />;
};

const IncrementButton = props => {
  return <Button icon='chevron-up' {...props} />;
};

const PrimaryButton = (props = {}) => {
  return (
    <Button {...props} className={ classNames('fc-btn-primary', props.className) }>
      {props.children}
    </Button>
  );
};

PrimaryButton.propTypes = {
  children: PropTypes.node
};

export {
  Button,
  LeftButton,
  RightButton,
  DecrementButton,
  DeleteButton,
  EditButton,
  AddButton,
  IncrementButton,
  PrimaryButton
};
