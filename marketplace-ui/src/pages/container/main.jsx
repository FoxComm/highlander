/* @flow */

import React from 'react';

import type { HTMLElement } from '../../core/types';

import styles from './main.css';

type Props = {
  children?: HTMLElement;
}

export default (props: Props) => (
  <main className={styles.main}>
    {props.children}
  </main>
);
