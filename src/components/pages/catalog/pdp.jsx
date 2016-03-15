/* @flow */

import _ from 'lodash';
import React, { Component } from 'react';
import styles from './pdp.css';
import { connect } from 'react-redux';

import Button from 'ui/buttons';
import Counter from 'ui/forms/counter';
import Currency from 'ui/currency';
import { Link } from 'react-router';

import * as actions from 'modules/product-details';

import type { ProductResponse } from 'modules/product-details';

type Params = {
  productId: string,
};

type Props = {
  fetch: (id: number) => any,
  params: Params,
  product: ProductResponse,
};

const getState = state => ({ product: state.productDetails.product });

class Pdp extends Component<void, Props, void> {
  componentWillMount() {
    this.props.fetch(this.productId);
  }

  get productId(): number {
    return parseInt(this.props.params.productId, 10);
  }

  render() {
    const { product } = this.props;
    if (!product) {
      return <div></div>;
    }

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
            <Link to="/" styleName="breadcrumb">&lt; BACK</Link>
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

export default connect(getState, actions)(Pdp);
