/* @flow */

import React, { Component } from 'react';
import { findDOMNode } from 'react-dom';

import styles from './site.css';

import type { HTMLElement } from '../../core/types';

type Props = {
  children?: HTMLElement;
}

class Site extends Component {
  props: Props;

  componentDidMount() {
    const node = findDOMNode(this);
    const enhancedClass = styles.siteLoaded;
    const img = new Image();

    img.onload = function() {
      node.className += ` ${enhancedClass}`;
    };

    img.src = '/images/bg.jpg';
  }

  get footer() {
    return (
      <footer className={styles.footer}>
        Any questions? Contact our <a href="mailto:support@foxcommerce.com" target="blank">Support Team</a>
      </footer>
    );
  }

  render(): HTMLElement {
    return (
      <div className={styles.site}>
        {this.props.children}
        {this.footer}
      </div>
    );
  }
}

export default Site;
