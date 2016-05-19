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
            link={{ to: '#', text: 'shop' }}
          />
        </div>
      </div>
    );
  }

  get greyson(): HTMLElement {
    return (
      <div styleName="greyson-block">
        <div styleName="wrap">
          <CollectionBanner
            header="Greyson"
            description="on him in gold"
            link={{ to: '#', text: 'shop' }}
          />
        </div>
      </div>
    );
  }

  get chloe(): HTMLElement {
    return (
      <div styleName="chloe-block">
        <div styleName="wrap">
          <CollectionBanner
            header="Chloe"
            description="on her in black"
            link={{ to: '#', text: 'shop' }}
          />
        </div>
      </div>
    );
  }

  get amelia(): HTMLElement {
    return (
      <div styleName="amelia-block">
        <div styleName="wrap">
          <CollectionBanner
            header="Amelia"
            description="on her in tortoise"
            link={{ to: '#', text: 'shop' }}
          />
        </div>
      </div>
    );
  }

  get olivia(): HTMLElement {
    return (
      <div styleName="olivia-block">
        <div styleName="wrap">
          <CollectionBanner
            header="Connor"
            description="on him in black"
            link={{ to: '#', text: 'shop' }}
          />
        </div>
      </div>
    );
  }

  get connor(): HTMLElement {
    return (
      <div styleName="connor-block">
        <div styleName="wrap">
          <CollectionBanner
            header="Olivia"
            description="on her in block"
            link={{ to: '#', text: 'shop' }}
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
        {this.greyson}
        {this.chloe}
        {this.amelia}
        {this.olivia}
        {this.connor}
      </div>
    );
  }
}

export default Collection;
