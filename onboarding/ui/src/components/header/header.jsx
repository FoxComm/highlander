import cx from 'classnames';
import React from 'react';

import styles from './header.css';

import type { HTMLElement } from '../../core/types';

type Props = {
  title: string;
  legend: string;
  className?: string;
}

export default (props: Props): HTMLElement => (
  <div className={cx(styles.header, props.className)}>
    <h1 className={styles.title}>{props.title}</h1>
    <p className={styles.legend}>{props.legend}</p>
  </div>
);
