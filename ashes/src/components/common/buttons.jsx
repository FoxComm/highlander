// libs
import React, { PropTypes } from 'react';
import classNames from 'classnames';

// styles
import s from './buttons.css';

const Button = (props = {}) => {
  const { icon, inline, docked, children, isLoading, ...restProps } = props;
  const className = classNames(
    'fc-btn',
    s.block,
    {
      '_docked-left': docked === 'left',
      '_docked-right': docked === 'right',
      '_loading': isLoading,
    },
    props.className,
  );

  const content = children != null ? <span className={s.text}>{children}</span> : children;

  return (
    <button {...restProps} className={className}>
      {icon && <i className={`icon-${icon}`}/>}
      {content}
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
  isLoading: PropTypes.bool,
  inline: PropTypes.bool,
};

Button.defaultProps = {
  inline: false,
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

const CloseButton = props => {
  return <Button icon='close' {...props} />;
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
  IncrementButton,
  DeleteButton,
  EditButton,
  AddButton,
  CloseButton,
  PrimaryButton,
};
