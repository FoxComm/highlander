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
          <div styleName="top-banner-wrap">
            <div styleName="top-banner-info">
              <div styleName="top-banner-header">Summer 2016</div>
              <div styleName="top-banner-description">New collection is here</div>
              <div styleName="top-banner-links">
                <Link to="/all?shop=men" styleName="top-banner-shop-link">
                  Shop Men
                </Link>
                <Link to="/all?shop=women" styleName="top-banner-shop-link">
                  Shop Women
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }
}

export default Main;
