import classNames from 'classnames/dedupe';
import React from 'react';

import styles from './css/buttons.css';
import type { HTMLElement } from 'types';

import Icon from 'ui/icon';

type ButtonProps = {
  children: HTMLElement|string;
  icon?: string;
  isLoading?: boolean;
  className?: string;
}

const Button = (props: ButtonProps) => {
  const { isLoading, className, disabled, ...rest } = props;
  let { icon } = props;

  if (icon) {
    icon = <Icon name={icon} />;
  }

  const cls = classNames(styles[isLoading ? 'button-loading' : 'button'], className);

  return (
    <button className={cls} disabled={isLoading || disabled} {...rest}>
      {icon}
      {props.children}
    </button>
  );
};


export default Button;
