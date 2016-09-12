/* @flow */

import React, { Component } from 'react';

import styles from './site.css';

import type { HTMLElement } from '../../core/types';

type Props = {
  children?: HTMLElement;
}

class Site extends Component {
  props: Props;

  render(): HTMLElement {
    return (
      <div className={styles.site}>
        {this.props.children}
      </div>
    );
  }
}

export default Site;
