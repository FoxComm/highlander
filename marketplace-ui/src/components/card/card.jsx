/* flow */

import React from 'react';

import Button from '../button/button';

import styles from './card.css';

import type { HTMLElement } from '../../core/types';

type Props = {
  title: string;
  description: string;
  button?: string;
  onSelect: Function
}

export default ({ title, description, button, onSelect }: Props): HTMLElement => (
  <div className={styles.card}>
    <div className={styles.title}>{title}</div>
    <div className={styles.description}>{description}</div>
    <Button className={styles.button} onClick={onSelect}>{button}</Button>
  </div>
);
