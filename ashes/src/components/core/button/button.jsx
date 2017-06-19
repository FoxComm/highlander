/* @flow */

// libs
import classNames from 'classnames';
import React, { Element } from 'react';

// components
import Icon from 'components/core/icon';

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
  fullWidth?: boolean,
  /** Small theme for button */
  small?: boolean,
};

/**
 * Button component has a bunch of helper components built on top of generic `Button` component.
 * Usage is the same across all buttons.
 *
 * [Mockups](https://zpl.io/Z39JBU)
 *
 * @function Button
 */
export const Button = ({ icon, children, isLoading, className, fullWidth, small, ...restProps }: Props) => {
  const hasIcon = !!icon;
  const content = children ? <span>{children}</span> : null;
  const disabled = restProps.disabled || isLoading;
  const onlyIcon = hasIcon && !content;
  const cls = classNames(
    s.button,
    {
      [s.loading]: isLoading,
      [s.fullWidth]: fullWidth,
      [s.small]: small,
      [s.onlyIcon]: onlyIcon
    },
    className
  );
  const postfix = icon || '';
  const iconCls = classNames(s.icon, {
    [`icon-${postfix}`]: hasIcon,
    [s.only]: onlyIcon,
  });

  return (
    <button {...restProps} className={cls} disabled={disabled}>
      {hasIcon && <Icon name={iconCls} />}
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

type SocialProps = Props & {
  type: 'google',
};

export const SocialButton = ({ className, type, ...rest }: SocialProps) => {
  return <Button icon={type} {...rest} className={classNames(s.socialButton, s[type], className)} />;
};
