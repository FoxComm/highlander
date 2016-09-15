/* @flow */

import cx from 'classnames';
import React from 'react';

import styles from './button.css';

import type { HTMLElement } from '../../core/types';

type Props = {
  active: boolean;
  children?: HTMLElement;
}

export default (props: Props): HTMLElement => {
  const { active, children } = props;

  const cls = cx(
    styles.button,
    { [styles.buttonActive]: active }
  );

  return (
    <button className={cls} type="submit">{children}</button>
  );
};
