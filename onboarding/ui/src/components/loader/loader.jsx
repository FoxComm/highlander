/* @flow */

import React from 'react';

import styles from './loader.css';

import type { HTMLElement } from '../../core/types';

export default (): HTMLElement => (
  <div className={styles.loader}>
    <span />
    <span />
    <span />
    <span />
    <span />
  </div>
);
