/* @flow */

import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { browserHistory } from 'react-router';

import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

import styles from './pdp.css';

import Button from 'ui/buttons';
import Counter from 'ui/forms/counter';
import Currency from 'ui/currency';
import { Link } from 'react-router';
import Gallery from 'ui/gallery/gallery';
import Loader from 'ui/loader';

import * as actions from 'modules/product-details';
import { fetch as fetchProducts } from 'modules/products';
import { addLineItem, toggleCart } from 'modules/cart';

import type { HTMLElement } from 'types';
import type { ProductResponse } from 'modules/product-details';

type Params = {
  productId: string;
};

type Props = Localized & {
  fetch: (id: number) => any;
  params: Params;
  product: ProductResponse|null;
  auth: any;
  addLineItem: Function;
  toggleCart: Function;
  resetProduct: Function;
  getNextId: Function;
  getPreviousId: Function;
  fetchProducts: Function;
  isLoading: boolean;
  isCartLoading: boolean;
  notFound: boolean;
};

type State = {
  quantity: number;
}

const mapStateToProps = state => {
  const product = state.productDetails.product;

  return {
    product,
    auth: state.auth,
    notFound: !product && _.get(state.asyncActions, ['pdp', 'err', 'status']) == 404,
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
    this.props.fetchProducts();
    if (!this.props.product) {
      this.props.fetch(this.productId);
    }
  }

  componentWillUnmount() {
    this.props.resetProduct();
  }

  componentWillUpdate(nextProps) {
    const stringId = nextProps.params.productId;
    const id = parseInt(stringId, 10);
    if (this.productId !== id) {
      this.props.fetch(id);
    }
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
    const user = _.get(this.props, ['auth', 'user'], null);
    if (!_.isEmpty(user)) {
      const quantity = this.state.quantity;
      const skuId = this.firstSqu;
      this.props.addLineItem(skuId, quantity).then(() => {
        this.props.toggleCart();
        this.setState({quantity: 1});
      });
    } else {
      browserHistory.push({
        pathname: `/products/${this.productId}`,
        query: { auth: 'login' },
      });
    }
  }

  get pathToNext(): string {
    const nextId = this.props.getNextId(this.productId);

    if (nextId == null) {
      return '/';
    }

    return `/products/${nextId}`;
  }

  get pathToPrevious(): string {
    const prevId = this.props.getPreviousId(this.productId);

    if (prevId == null) {
      return '/';
    }

    return `/products/${prevId}`;
  }

  render(): HTMLElement {
    const { t } = this.props;

    if (this.props.isLoading) {
      return <Loader/>;
    }

    if (this.props.notFound) {
      return <p styleName="not-found">{t('Product not found')}</p>;
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
            <Link to="/" styleName="breadcrumb">{t('SHOP')}</Link>
            &nbsp;/&nbsp;
            <Link to={`/products/${this.productId}`} styleName="breadcrumb">{title.toUpperCase()}</Link>
          </div>
          <div styleName="mobile-links">
            <Link to={this.pathToPrevious} styleName="breadcrumb">{t('< BACK')}</Link>
          </div>
          <div>
            <Link to={this.pathToNext} styleName="breadcrumb">{t('NEXT >')}</Link>
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
              <label>{t('QUANTITY')}</label>
              <div styleName="counter">
                <Counter
                  value={this.state.quantity}
                  decreaseAction={() => this.onQuantityChange(-1)}
                  increaseAction={() => this.onQuantityChange(1)}
                />
              </div>
            </div>
            <Button styleName="add-to-cart" isLoading={isCartLoading} onClick={this.addToCart}>
              {t('ADD TO CART')}
            </Button>
          </div>
        </div>
      </div>
    );
  }
}

export default connect(mapStateToProps, {...actions, addLineItem, toggleCart, fetchProducts})(localized(Pdp));
