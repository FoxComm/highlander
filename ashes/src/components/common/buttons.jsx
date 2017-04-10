/* @flow */

// libs
import classNames from 'classnames';
import React, { Element } from 'react';

// styles
import s from './buttons.css';

type Props = {
  className?: string,
  icon?: string,
  isLoading?: boolean,
  children: Element<*>,
}

export const Button = (props: Props = {}) => {
  const { icon, children, isLoading, ...restProps } = props;
  const className = classNames(
    s.button,
    { [s.loading]: isLoading },
    props.className
  );

  const content = children ? <span className={s.text}>{children}</span> : null;

  return (
    <button {...restProps} className={className}>
      {icon && <i className={`icon-${icon}`} />}
      {content}
    </button>
  );
};

export const PrimaryButton = ({ className, ...rest }: Props) => {
  return (
    <Button {...rest} className={classNames(s.primary, className)} />
  );
};

export const LeftButton = (props: Props) => {
  return <Button icon='chevron-left' {...props} />;
};

export const RightButton = (props: Props) => {
  return <Button icon='chevron-right' {...props} />;
};

export const DecrementButton = (props: Props) => {
  return <Button icon='chevron-down' {...props} />;
};

export const IncrementButton = (props: Props) => {
  return <Button icon='chevron-up' {...props} />;
};

export const AddButton = (props: Props) => {
  return <Button icon='add' {...props} />;
};

export const EditButton = (props: Props) => {
  return <Button icon='edit' {...props} />;
};

export const DeleteButton = ({ className, ...rest }: Props) => {
  return <Button icon='trash' {...rest} className={classNames(s.delete, className)} />;
};

export const CloseButton = ({ className, ...rest }: Props) => {
  return <Button icon='close' {...rest} className={classNames(s.close, className)} />;
};
