/* @flow */

import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import type { HTMLElement } from 'types';

import styles from './pdp.css';

import Button from 'ui/buttons';
import Counter from 'ui/forms/counter';
import Currency from 'ui/currency';
import { Link } from 'react-router';
import Gallery from 'ui/gallery/gallery';
import Loader from 'ui/loader';

import * as actions from 'modules/product-details';
import { addLineItem, toggleCart } from 'modules/cart';

import type { ProductResponse } from 'modules/product-details';

type Params = {
  productId: string;
};

type Props = {
  fetch: (id: number) => any;
  params: Params;
  product: ProductResponse|null;
  addLineItem: Function;
  toggleCart: Function;
  resetProduct: Function;
  isLoading: boolean;
  notFound: boolean;
};

type State = {
  quantity: number;
}

const getState = state => {
  const product = state.productDetails.product;

  return {
    product,
    notFound: !product && _.get(state.asyncActions, ['pdp', 'failed'], false),
    isLoading: _.get(state.asyncActions, ['pdp', 'inProgress'], true),
  };
};

class Pdp extends Component {
  props: Props;

  state: State = {
    quantity: 1,
  };

  componentWillMount() {
    /** prevent load on client on mount */
    if (!this.props.product) {
      this.props.fetch(this.productId);
    }
  }

  componentWillUnmount() {
    this.props.resetProduct();
  }

  get productId(): number {
    return parseInt(this.props.params.productId, 10);
  }

  get firstSqu(): string {
    return _.get(this.props, ['product', 'skus', 0, 'code']);
  }

  @autobind
  onQuantityChange(value): void {
    const newValue = this.state.quantity + value;
    if (newValue > 0) {
      this.setState({quantity: newValue});
    }
  }

  @autobind
  addToCart(): void {
    const quantity = this.state.quantity;
    const skuId = this.firstSqu;
    this.props.addLineItem(skuId, quantity).then(() => {
      this.props.toggleCart();
      this.setState({quantity: 1});
    });
  }

  render(): HTMLElement {
    if (this.props.isLoading) {
      return <Loader/>;
    }

    if (this.props.notFound) {
      return <p styleName="not-found">Product not found</p>;
    }

    const { product } = this.props;

    const title = _.get(product, ['product', 'attributes', 'title', 'v'], '');
    const description = _.get(product, ['product', 'attributes', 'description', 'v'], '');
    const price = _.get(product, ['skus', 0, 'attributes', 'price', 'v', 'value'], 0);
    const currency = _.get(product, ['skus', 0, 'attributes', 'price', 'v', 'currency'], 'USD');
    const imageUrls = _.get(product, ['product', 'attributes', 'images', 'v'], []);
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
            <Gallery images={imageUrls} />
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
                <Counter
                  value={this.state.quantity}
                  decreaseAction={() => this.onQuantityChange(-1)}
                  increaseAction={() => this.onQuantityChange(1)}
                />
              </div>
            </div>
            <div styleName="add-to-cart">
              <Button onClick={this.addToCart}>ADD TO CART</Button>
            </div>
          </div>
        </div>
      </div>
    );
  }
}

export default connect(getState, {...actions, addLineItem, toggleCart})(Pdp);
