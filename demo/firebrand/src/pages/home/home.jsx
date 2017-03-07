/* @flow */

import React, { Component } from 'react';
import type { HTMLElement } from 'types';

import Banner from '../../components/banner/banner';
import Category from './category';
import styles from './home.css';


class Home extends Component {

  get topBanner(): HTMLElement {
    return (
      <div styleName="top-banner">
        <div styleName="wrap">
          <Banner
            header="Summer 2016"
            description="New collection is here"
            links={[
              {to: '/collections/summer2016?type=men', text: 'Shop Men'},
              {to: '/collections/summer2016?type=women', text: 'Shop Women'},
            ]}
          />
        </div>
      </div>
    );
  }

  get categories(): HTMLElement {
    return (
      <div styleName="categories">
        <Category
          name="EYEGLASSES"
          category="eyeglasses"
          image="images/banners/eyeglasses.jpg"
        />
        <Category
          name="SUNGLASSES"
          category="sunglasses"
          image="images/banners/sunglasses.jpg"
        />
      </div>
    );
  }

  get tryAtHomeBanner(): HTMLElement {
    return (
      <div styleName="try-at-home-banner">
        <div styleName="wrap">
          <Banner
            header="Try @ home"
            description="5 pairs. 5 days. Free"
            links={[
              {to: '/collections/summer2016', text: 'Learn More'},
            ]}
          />
        </div>
      </div>
    );
  }

  get visitAStoreBanner(): HTMLElement {
    return (
      <div styleName="visit-a-store-banner">
        <div styleName="wrap">
          <Banner
            header="Visit a store"
            description="Try them on in person!"
            links={[
              {to: 'locations', text: 'Locations'},
            ]}
          />
          <div styleName="visit-a-store-banner__image"></div>
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

export default Home;
