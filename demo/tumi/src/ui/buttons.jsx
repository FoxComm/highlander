// @flow
import classNames from 'classnames/dedupe';
import React, { Element } from 'react';

import styles from './css/buttons.css';

import Icon from 'ui/icon';

type ButtonProps = {
  icon?: string,
  isLoading?: boolean|null,
  className?: string,
  disabled?: boolean,
  children?: Element<*>|string,
  onClick?: (event: SyntheticEvent) => void,
};

export const Button = (props: ButtonProps) => {
  const { isLoading, className, disabled, ...rest } = props;
  let { icon } = props;

  if (icon) {
    icon = <Icon name={icon} />;
  }

  const cls = classNames(styles.button, className, {
    [styles._loading]: isLoading,
  });

  return (
    <button className={cls} disabled={isLoading || disabled} {...rest}>
      {icon}
      {props.children}
    </button>
  );
};

export const SecondaryButton = (props: ButtonProps) => {
  const { className, ...rest } = props;
  return (
    <Button className={classNames(styles._secondary, className)} {...rest} />
  );
};

export default Button;
