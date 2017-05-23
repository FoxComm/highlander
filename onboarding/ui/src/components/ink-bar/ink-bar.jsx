/* flow */

import React from 'react';

import styles from './ink-bar.css';

import type { HTMLElement } from '../../core/types';

type Props = {
  width: string;
  left: string;
}

export default ({ width, left }: Props): HTMLElement => (
  <div style={{ width, transform: `translateX(${left})` }} className={styles.inkBar} />
);

