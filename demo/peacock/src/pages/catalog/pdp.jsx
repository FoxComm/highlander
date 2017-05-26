/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// i18n
import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

// modules
import { searchGiftCards } from 'modules/products';
import { fetch, getNextId, getPreviousId, resetProduct } from 'modules/product-details';
import { addLineItem, toggleCart } from 'modules/cart';
import { fetchRelatedProducts, clearRelatedProducts } from 'modules/cross-sell';

// styles
import styles from './pdp.css';

// components
import { Pdp, RelatedProductList } from '@foxcomm/sf-react';

// types
import type { Product } from '@foxcomm/api-js/types/api/product';
import type { RelatedProductResponse } from 'modules/cross-sell';
import type { RoutesParams } from 'types';

type Params = {
  productSlug: string,
};

type Actions = {
  fetch: (id: number) => any,
  getNextId: Function,
  getPreviousId: Function,
  resetProduct: Function,
  addLineItem: Function,
  toggleCart: Function,
  fetchRelatedProducts: Function,
  clearRelatedProducts: Function,
};

type Props = Localized & RoutesParams & {
  actions: Actions,
  params: Params,
  product: ?Product,
  isLoading: boolean,
  notFound: boolean,
  relatedProducts: ?RelatedProductResponse,
};

const mapStateToProps = (state) => {
  const product = state.productDetails.product;
  const relatedProducts = state.crossSell.relatedProducts;

  return {
    product,
    relatedProducts,
    fetchError: _.get(state.asyncActions, 'pdp.err', null),
    notFound: !product && _.get(state.asyncActions, 'pdp.err.response.status') == 404,
    isLoading: _.get(state.asyncActions, ['pdp', 'inProgress'], true),
    isRelatedProductsLoading: _.get(state.asyncActions, ['relatedProducts', 'inProgress'], false),
  };
};

const mapDispatchToProps = dispatch => ({
  actions: bindActionCreators({
    fetch,
    getNextId,
    getPreviousId,
    resetProduct,
    addLineItem,
    toggleCart,
    fetchRelatedProducts,
    clearRelatedProducts,
  }, dispatch),
});

class PdpConnect extends Component {
  props: Props;
  productPromise: Promise<*>;

  componentWillMount() {
    if (_.isEmpty(this.props.product)) {
      this.productPromise = this.fetchProduct();
    } else {
      this.productPromise = Promise.resolve();
    }
  }

  componentDidMount() {
    this.productPromise.then(() => {
      const { product, isRelatedProductsLoading, actions } = this.props;
      if (!isRelatedProductsLoading) {
        actions.fetchRelatedProducts(product.id, 1).catch(_.noop);
      }
    });
  }

  componentWillUnmount() {
    this.props.actions.resetProduct();
    this.props.actions.clearRelatedProducts();
  }

  componentWillUpdate(nextProps) {
    const nextId = this.getId(nextProps);

    if (this.productId !== nextId) {
      this.props.actions.resetProduct();
      this.props.actions.clearRelatedProducts();
      this.fetchProduct(nextProps, nextId);
    }
  }

  safeFetch(id) {
    return this.props.actions.fetch(id)
      .then((product) => {
        this.props.actions.fetchRelatedProducts(product.id, 1).catch(_.noop);
      })
      .catch(() => {
        const { params } = this.props;
        this.props.actions.fetch(params.productSlug)
        .then((product) => {
          this.props.actions.fetchRelatedProducts(product.id, 1).catch(_.noop);
        });
      });
  }

  fetchProduct(_props, _productId) {
    const props = _props || this.props;
    const productId = _productId || this.productId;

    if (this.isGiftCardRoute(props)) {
      return searchGiftCards().then(({ result = [] }) => {
        const giftCard = result[0] || {};
        return this.safeFetch(giftCard.productId);
      });
    }
    return this.safeFetch(productId);
  }

  get productId(): string|number {
    return this.getId(this.props);
  }

  @autobind
  getId(props): string|number {
    const slug = props.params.productSlug;

    if (/^\d+$/g.test(slug)) {
      return parseInt(slug, 10);
    }

    return slug;
  }

  isGiftCardRoute(props = this.props) {
    return props.route.name === 'gift-cards';
  }

  @autobind
  handleAddToCard(skuCode: string, quantity: number, attributes: Object) {
    const { actions } = this.props;

    return actions.addLineItem(skuCode, quantity, attributes).then(() => {
      actions.toggleCart();
    });
  }

  get relatedProductsList(): ?Element<*> {
    const { relatedProducts, isRelatedProductsLoading } = this.props;

    if (_.isEmpty(relatedProducts.products)) return null;

    return (
      <RelatedProductList
        title="You Might Also Like"
        list={relatedProducts.products}
        isLoading={isRelatedProductsLoading}
      />
    );
  }

  render(): Element<any> {
    const {
      t,
      isLoading,
      notFound,
      fetchError,
      product,
    } = this.props;

    const pdpProps = { t, isLoading, notFound, fetchError, product };

    return (
      <Pdp
        {...pdpProps}
        relatedProductsList={this.relatedProductsList}
        shareImage={<img styleName="share-image" src="/images/pdp/style.jpg" />}
        onAddLineItem={this.handleAddToCard}
      />
    );
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(localized(PdpConnect));
