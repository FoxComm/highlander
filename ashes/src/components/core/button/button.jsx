/* @flow */

// libs
import classNames from 'classnames';
import React, { Element } from 'react';

// styles
import s from './button.css';

type Props = {
  /** Icon name that is used to be rendered in a button */
  icon?: string,
  /** If to show loading animation */
  isLoading?: boolean,
  /** Additional className */
  className?: string,
  /** Button content (label) */
  children?: Element<any>,
  /** If true — sets `width` style to 100% */
  stretch?: boolean,
  /** Google theme for button */
  google?: boolean,
  /** Small theme for button */
  small?: boolean,
};

/**
 * Button component has a bunch of helper components built on top of generic `Button` component.
 * Usage is the same across all buttons.
 *
 * @function Button
 */
export const Button = ({ icon, children, isLoading, className, stretch, google, small, ...restProps }: Props) => {
  const hasIcon = !!icon;
  const content = children ? <span className={s.text}>{children}</span> : null;
  const cls = classNames(
    s.button,
    {
      [s.loading]: isLoading,
      [s.stretch]: stretch,
      [s.google]: google,
      [s.small]: small,
      [s.onlyIcon]: hasIcon && !content
    },
    className
  );

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
