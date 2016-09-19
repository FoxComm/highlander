/* @flow */

import cx from 'classnames';
import React, { Component } from 'react';

import styles from './thanks-or-not.css';

type Props = {
  title?: string;
  message?: string;
  error?: boolean;
}

class ThanksOrNot extends Component {
  props: Props;

  static defaultProps = {
    title: 'Thank You!',
    message: 'We\'ll call you soon.',
    error: false,
  };

  render() {
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
