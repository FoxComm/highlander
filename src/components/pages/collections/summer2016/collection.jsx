/* @flow */

import React, { Component } from 'react';
import type { HTMLElement } from 'types';

import styles from './collection.css';

import CollectionBanner from '../../../banner/collection-banner';
import TextBanner from '../../../banner/text-banner';


class Collection extends Component {

  get hero(): HTMLElement {
    return (
      <div styleName="hero-block">
        <div styleName="wrap">
          <TextBanner header="Summer 2016">
            <p>Something about super awesome summer collection.</p>
            <p>Now get out there and get after it.</p>
          </TextBanner>
        </div>
      </div>
    );
  }

  get harper(): HTMLElement {
    return (
      <div styleName="harper-block">
        <div styleName="wrap">
          <CollectionBanner
            header="Harper"
            description="on her in honey"
            link={{ to: '/products/7', text: 'shop' }}
          />
        </div>
      </div>
    );
  }

  render(): HTMLElement {
    return (
      <div>
        {this.hero}
        {this.harper}
      </div>
    );
  }
}

export default Collection;
