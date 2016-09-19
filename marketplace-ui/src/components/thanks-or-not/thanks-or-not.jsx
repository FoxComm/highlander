/* @flow */

import cx from 'classnames';
import React, { Component } from 'react';

import styles from './thanks-or-not.css';

import type { HTMLElement } from '../../core/types';

type Props = {
  title?: HTMLElement;
  message: HTMLElement;
  error?: boolean;
  className?: string;
  children?: HTMLElement;
}

class ThanksOrNot extends Component {
  props: Props;

  static defaultProps = {
    title: 'Thank You!',
    error: false,
  };

  render(): HTMLElement {
    const { error, title, message, className, children } = this.props;

    const cls = cx(styles.thanksOrNot, className);
    const clsIcon = cx(styles.icon, {
      [styles.thanks]: !error,
      [styles.error]: error,
    });

    const content = children || <div className={clsIcon} />;

    return (
      <div className={cls}>
        {content}
        <div className={styles.title}>{title}</div>
        <div className={styles.message}>{message}</div>
      </div>
    );
  }
}

export default ThanksOrNot;
