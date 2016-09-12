/* @flow */

import React from 'react';

import styles from './site.css';

import type { HTMLElement } from '../../core/types';

type Props = {
  children?: HTMLElement;
}

export default (props: Props) => (
  <div className={styles.site}>
    {props.children}
  </div>
);
