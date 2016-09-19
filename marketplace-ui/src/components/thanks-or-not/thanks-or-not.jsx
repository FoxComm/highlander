/* @flow */

import cx from 'classnames';
import React, { Component } from 'react';

import styles from './thanks-or-not.css';

import type { HTMLElement } from '../../core/types';

type Props = {
  title?: HTMLElement;
  message: HTMLElement;
  error?: boolean;
}

class ThanksOrNot extends Component {
  props: Props;

  static defaultProps = {
    title: 'Thank You!',
    error: false,
  };

  render(): HTMLElement {
    const { error, title, message } = this.props;

    const clsIcon = cx(styles.icon, {
      [styles.thanks]: !error,
      [styles.error]: error,
    });

    return (
      <div className={styles.thanksOrNot}>
        <div className={clsIcon} />
        <div className={styles.title}>{title}</div>
        <div className={styles.message}>{message}</div>
      </div>
    );
  }
}

export default ThanksOrNot;
