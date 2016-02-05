
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import _ from 'lodash';

const Button = (props = {}) => {
  const {icon, inline, docked, children, ...restProps} = props;
  const className = classNames(
    'fc-btn',
    {'_docked-left': docked && docked === 'left'},
    {'_docked-right': docked && docked === 'right'},
    props.className,
  );

  return (
    <button {...restProps} className={className}>
      {icon && <i className={`icon-${icon}`}></i>}
      {children}
    </button>
  );
};

Button.propTypes = {
  className: PropTypes.string,
  docked: PropTypes.oneOf([
    'left',
    'right',
  ]),
  icon: PropTypes.string,
  children: PropTypes.node,
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

const IncrementButton = props => {
  return <Button icon='chevron-up' {...props} />;
};

const DeleteButton = (props = {}) => {
  return <Button icon='trash' {...props} className={ classNames('fc-btn-remove', props.className) } />;
};

DeleteButton.propTypes = {
  className: PropTypes.string,
};

const EditButton = props => {
  return <Button icon='edit' {...props} />;
};

const AddButton = props => {
  return <Button icon='add' {...props} />;
};

const PrimaryButton = (props = {}) => {
  return (
    <Button {...props} className={ classNames('fc-btn-primary', props.className) }>
      {props.children}
    </Button>
  );
};

PrimaryButton.propTypes = {
  children: PropTypes.node,
  className: PropTypes.string,
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
  PrimaryButton,
};
