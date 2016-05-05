/* @flow */

import React, { Component } from 'react';
import type { HTMLElement } from 'types';
import { Link } from 'react-router';

import styles from './main.css';


class Main extends Component {

  render(): HTMLElement {
    return (
      <div>
        <div styleName="top-banner">
          <span styleName="top-banner-header"></span>
          <span styleName="top-banner-description"></span>
          <Link to="/all?shop=men" styleName="top-banner-shop-link">
            Shop Men
          </Link>
          <Link to="/all?shop=women" styleName="top-banner-shop-link">
            Shop Women
          </Link>
        </div>
      </div>
    );
  }
}

export default Main;
