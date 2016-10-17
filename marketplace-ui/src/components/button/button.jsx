/* @flow */

import cx from 'classnames';
import React from 'react';

import styles from './button.css';

import type { HTMLElement } from '../../core/types';

type Props = {
  active: boolean;
  disabled?: boolean;
  type?: string;
  onClick?: Function;
  className?: srting;
  children?: HTMLElement;
}

export default (props: Props): HTMLElement => {
  const { active, disabled, type, onClick, className, children } = props;

  const cls = cx(styles.button, className, { [styles.buttonActive]: active });

  return (
    <button className={cls} type={type} onClick={onClick} disabled={disabled}>{children}</button>
  );
};
