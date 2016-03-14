/* @flow */

import _ from 'lodash';
import React, { Component } from 'react';
import cssModules from 'react-css-modules';
import styles from './pdp.css';

import Button from 'ui/buttons';
import Counter from 'ui/forms/counter';
import Currency from 'ui/currency';
import { Link } from 'react-router';

const data = {
  id: 1,
  context: {
    name: 'default',
    attributes: {
      language: 'EN',
      modality: 'desktop',
    },
  },
  product: {
    id: 1,
    attributes: {
      title: {
        t: 'string',
        v: 'Flonkey',
      },
      images: {
        t: 'images',
        v: [
          'http://lorempixel.com/75/75/fashion/',
        ],
      },
      description: {
        t: 'string',
        v: 'Best in Class Flonkey',
      },
    },
    variants: {
      default: {},
    },
    skus: {
      default: {
        'SKU-YAX': {},
      },
    },
    activeFrom: '2016-03-14T18:18:47.187Z',
  },
  skus: [
    {
      code: 'SKU-YAX',
      attributes: {
        price: {
          t: 'price',
          v: {
            value: 3300,
            currency: 'USD',
          },
        },
        title: {
          t: 'string',
          v: 'Flonkey',
        },
      },
      activeFrom: '2016-03-14T18:18:47.231Z',
    },
  ],
};

class Pdp extends Component {
  render() {
    const product = data;
    const title = _.get(product, ['product', 'attributes', 'title', 'v'], '');
    const description = _.get(product, ['product', 'attributes', 'description', 'v'], '');
    const price = _.get(product, ['skus', 0, 'attributes', 'price', 'v', 'value'], 0);
    const currency = _.get(product, ['skus', 0, 'attributes', 'price', 'v', 'currency'], 'USD');
    const imageUrls = _.get(product, ['product', 'attributes', 'images', 'v'], []);
    const images = _.map(imageUrls, (url) => <img src={url} styleName="preview-image" />);
    return (
      <div styleName="container">
        <div styleName="links">
          <div styleName="desktop-links">
            <Link to="/" styleName="breadcrumb">SHOP</Link> / LOREM IPSUM
          </div>
          <div styleName="mobile-links">
            <Link to="/" styleName="breadcrumb">&lt; BACK </Link>
          </div>
          <div>
            NEXT &gt;
          </div>
        </div>
        <div styleName="details">
          <div styleName="images">
            {images}
          </div>
          <div styleName="info">
            <h1 styleName="name">{title}</h1>
            <div styleName="price">
              <Currency value={price} currency={currency} />
            </div>
            <div styleName="description">
              {description}
            </div>
            <div>
              <label>QUANTITY</label>
              <div styleName="counter">
                <Counter />
              </div>
            </div>
            <div styleName="add-to-cart">
              <Button>ADD TO CART</Button>
            </div>
          </div>
        </div>
      </div>
    );
  }
}

export default cssModules(Pdp, styles);
