/* @flow */

import React, { Component } from 'react';
import type { HTMLElement } from 'types';

import styles from './collection.css';


class OurStory extends Component {

  get hero(): HTMLElement {
    return (
      <div styleName="hero-banner"></div>
    );
  }

  render(): HTMLElement {
    return (
      <div>
        {this.hero}
      </div>
    );
  }
}

export default OurStory;
