/* @flow */

import React, { Component } from 'react';
import type { HTMLElement } from 'types';
import { Link } from 'react-router';

import styles from './main.css';


class Main extends Component {

  get topBanner(): HTMLElement {
    return (
      <div styleName="top-banner">
        <div styleName="block-wrap">
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
    );
  }

  get categories(): HTMLElement {
    return (
      <div styleName="categories">
        <div styleName="block-wrap">
          <div styleName="category-link">
            <div styleName="category-name">
              EYEGLASSES
            </div>
            <div styleName="category-image">
              <img src="images/banners/eyeglasses.jpg" />
            </div>
          </div>
          <div styleName="category-link">
            <div styleName="category-name">
              SUNGLASSES
            </div>
            <div styleName="category-image">
              <img src="images/banners/sunglasses.jpg" />
            </div>
          </div>
        </div>
      </div>
    );
  }

  get tryAtHomeBanner(): HTMLElement {
    return (
      <div styleName="try-at-home-banner">
        <div styleName="block-wrap">
          <div styleName="try-at-home-info">
            <div styleName="try-at-home-header">Try @ home</div>
            <div styleName="try-at-home-description">5 pairs. 5 days. Free</div>
            <Link to="/try-at-home" styleName="try-at-home-link">
              Learn More
            </Link>
          </div>
        </div>
      </div>
    );
  }

  get visitAStoreBanner(): HTMLElement {
    return (
      <div styleName="visit-a-store-banner">
        <div styleName="block-wrap">
          <div styleName="visit-a-store-info">
            <div styleName="visit-a-store-header">Visit a store</div>
            <div styleName="visit-a-store-description">Try them on in person!</div>
            <Link to="/locations" styleName="visit-a-store-link">
              Locations
            </Link>
          </div>
          <div styleName="visit-a-store-image"></div>
        </div>
      </div>
    );
  }

  render(): HTMLElement {
    return (
      <div>
        {this.topBanner}
        {this.categories}
        {this.tryAtHomeBanner}
        {this.visitAStoreBanner}
      </div>
    );
  }
}

export default Main;
