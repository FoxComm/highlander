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

type State = {
  quantity: number;
}

const getState = state => ({ product: state.productDetails.product });

class Pdp extends Component {
  state: State;
  props: Props;

  constructor(...args) {
    super(...args);
    this.state = {
      quantity: 1,
    };
  }

  componentWillMount() {
    this.props.fetch(this.productId);
  }

  get productId(): number {
    return parseInt(this.props.params.productId, 10);
  }

  @autobind
  onQuantityChange(value) {
    const newValue = this.state.quantity + value;
    if (newValue > 0) {
      this.setState({quantity: newValue});
    }
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
              <Button>ADD TO CART</Button>
            </div>
          </div>
        </div>
      </div>
    );
  }
}

export default connect(getState, actions)(Pdp);
