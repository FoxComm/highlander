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
  isCartLoading: boolean;
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
    isCartLoading: _.get(state.asyncActions, ['cartChange', 'inProgress'], false),
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

    const { product, isCartLoading } = this.props;

    const title = _.get(product, ['product', 'attributes', 'title', 'v'], '');
    const description = _.get(product, ['product', 'attributes', 'description', 'v'], '');
    const descriptionMarkup = { __html: description };
    const salePrice = _.get(product, ['skus', 0, 'attributes', 'salePrice', 'v', 'value'], 0);
    const currency = _.get(product, ['skus', 0, 'attributes', 'salePrice', 'v', 'currency'], 'USD');
    const imageUrls = _.get(product, ['product', 'attributes', 'images', 'v'], []);

    return (
      <div styleName="container">
        <div styleName="links">
          <div styleName="desktop-links">
            <Link to="/" styleName="breadcrumb">SHOP</Link>
            &nbsp;/&nbsp;
            <Link to={`/products/${this.productId}`} styleName="breadcrumb">{title.toUpperCase()}</Link>
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
            <div styleName="salePrice">
              <Currency value={salePrice} currency={currency} />
            </div>
            <div styleName="description" dangerouslySetInnerHTML={descriptionMarkup}>
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
            <Button styleName="add-to-cart" isLoading={isCartLoading} onClick={this.addToCart}>ADD TO CART</Button>
          </div>
        </div>
      </div>
    );
  }
}

export default connect(getState, {...actions, addLineItem, toggleCart})(Pdp);
