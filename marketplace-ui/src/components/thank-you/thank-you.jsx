/* @flow */

import React, { Component } from 'react';

import styles from './thank-you.css';

type Props = {
  title?: string;
  message?: string;
}

class ThankYou extends Component {
  props: Props;

  static defaultProps = {
    title: 'Thank You!',
    message: 'We\'ll call you soon.',
  };

  render() {
    return (
      <div className={styles.thankYou}>
        <div className={styles.icon} />
        <div className={styles.title}>{this.props.title}</div>
        <div className={styles.message}>{this.props.message}</div>
      </div>
    );
  }
}

export default ThankYou;
