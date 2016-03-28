/* @flow */

import _ from 'lodash';
import React, { Component } from 'react';
import styles from './pdp.css';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

import Button from 'ui/buttons';
import Counter from 'ui/forms/counter';
import Currency from 'ui/currency';
import { Link } from 'react-router';
import Gallery from 'ui/gallery/gallery';
import Loader from 'ui/loader';

import * as actions from 'modules/product-details';
import { addLineItem } from 'modules/cart';

import type { ProductResponse } from 'modules/product-details';

type Params = {
  productId: string;
};

type Props = {
  fetch: (id: number) => any;
  params: Params;
  product: ProductResponse;
  addLineItem: Function;
  isLoading: boolean;
};

type State = {
  quantity: number;
}

const getState = state => {
  const async = state.asyncActions.pdp;

  return {
    product: state.productDetails.product,
    isLoading: !!async ? async.inProgress : true,
  };
};

class Pdp extends Component {
  props: Props;

  state: State = {
    quantity: 1,
  };

  componentWillMount() {
    this.props.fetch(this.productId);
  }

  get productId(): number {
    return parseInt(this.props.params.productId, 10);
  }

  get firstSqu(): string {
    return _.get(this.props, ['product', 'skus', 0, 'code']);
  }

  @autobind
  onQuantityChange(value) {
    const newValue = this.state.quantity + value;
    if (newValue > 0) {
      this.setState({quantity: newValue});
    }
  }

  @autobind
  addToCart() {
    const quantity = this.state.quantity;
    const skuId = this.firstSqu;
    this.props.addLineItem(skuId, quantity);
  }

  render() {
    if (this.props.isLoading) {
      return <Loader/>;
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

export default connect(getState, {...actions, addLineItem})(Pdp);
