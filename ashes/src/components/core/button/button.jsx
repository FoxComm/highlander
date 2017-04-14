/* @flow */

// libs
import classNames from 'classnames';
import React, { Element } from 'react';

// styles
import s from './button.css';

type Props = {
  icon?: string,
  isLoading?: boolean,
  className?: string,
  children?: Element<any>,
}

export const Button = ({ icon, children, isLoading = false, className = '', ...restProps }: Props) => {
  const cls = classNames(
    s.button,
    { [s.loading]: isLoading },
    className
  );

  const content = children ? <span className={s.text}>{children}</span> : null;

  return (
    <button {...restProps} className={cls}>
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
