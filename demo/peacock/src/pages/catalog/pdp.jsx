/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';
import * as tracking from 'lib/analytics';

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

// types
import type { RelatedProductResponse } from 'modules/cross-sell';

// components
// import { SecondaryButton } from 'ui/buttons';
import AddToCartBtn from 'ui/add-to-cart-btn';
import Gallery from 'ui/gallery/gallery';
import Loader from 'ui/loader';
import Breadcrumbs from 'components/breadcrumbs/breadcrumbs';
import ErrorAlerts from '@foxcomm/wings/lib/ui/alerts/error-alerts';
import ProductDetails from './product-details';

import GiftCardForm from 'components/gift-card-form';
import ImagePlaceholder from 'components/products-item/image-placeholder';
import RelatedProductsList,
  { LoadingBehaviors } from 'components/related-products-list/related-products-list';

// types
import type { ProductResponse } from 'modules/product-details';
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
  product: ?ProductResponse,
  isLoading: boolean,
  isCartLoading: boolean,
  notFound: boolean,
  relatedProducts: ?RelatedProductResponse,
};

type State = {
  quantity: number,
  error?: any,
  currentSku?: any,
  attributes?: Object,
};

type Product = {
  title: string,
  description: string,
  images: Array<string>,
  currency: string,
  price: number|string,
  pathName: string,
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
    isCartLoading: _.get(state.asyncActions, ['cartChange', 'inProgress'], false),
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

class Pdp extends Component {
  props: Props;
  productPromise: Promise<*>;

  state: State = {
    quantity: 1,
    currentSku: null,
    attributes: {},
  };

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
      tracking.viewDetails(this.product);
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

  get isArchived(): boolean {
    return !!_.get(this.props, ['product', 'archivedAt']);
  }

  @autobind
  getId(props): string|number {
    const slug = props.params.productSlug;

    if (/^\d+$/g.test(slug)) {
      return parseInt(slug, 10);
    }

    return slug;
  }

  get currentSku() {
    return this.state.currentSku || this.sortedSkus[0];
  }

  get sortedSkus() {
    return _.sortBy(
      _.get(this.props, 'product.skus', []),
      'attributes.salePrice.v.value'
    );
  }

  @autobind
  setCurrentSku(currentSku) {
    this.setState({ currentSku });
  }

  @autobind
  setAttributeFromField({ target: { name, value } }) {
    const namePath = ['attributes', ...name.split('.')];
    const stateValue = name === 'giftCard.message' ? value.split('\n').join('<br>') : value;
    this.setState(assoc(this.state, namePath, stateValue));
  }

  get product(): Product {
    const attributes = _.get(this.props.product, 'attributes', {});
    const price = _.get(this.currentSku, 'attributes.salePrice.v', {});
    const images = _.get(this.props.product, ['albums', 0, 'images'], []);
    const imageUrls = images.map(image => image.src);

    return {
      title: _.get(attributes, 'title.v', ''),
      description: _.get(attributes, 'description.v', ''),
      images: imageUrls,
      currency: _.get(price, 'currency', 'USD'),
      price: _.get(price, 'value', 0),
      skus: this.sortedSkus,
      pathName: this.props.location.pathname,
    };
  }

  isGiftCardRoute(props = this.props) {
    return props.route.name === 'gift-cards';
  }

  isGiftCard(props = this.props): boolean {
    const tags = _.get(props.product, 'attributes.tags.v', []);
    return tags.indexOf('GIFT-CARD') !== -1;
  }

  @autobind
  changeQuantity(quantity: number): void {
    this.setState({ quantity });
  }

  @autobind
  addToCart(): void {
    const { actions } = this.props;
    const { quantity } = this.state;
    const skuId = _.get(this.currentSku, 'attributes.code.v', '');
    tracking.addToCart(this.product, quantity);
    actions.addLineItem(skuId, quantity, this.state.attributes)
      .then(() => {
        actions.toggleCart();
        this.setState({
          quantity: 1,
          attributes: {},
          currentSku: null,
        });
      })
      .catch((ex) => {
        this.setState({
          error: ex,
        });
      });
  }

  renderGallery() {
    const { images } = this.product;

    return !_.isEmpty(images)
      ? <Gallery images={images} />
      : <ImagePlaceholder largeScreenOnly />;
  }

  get productDetails(): Element<*> {
    const description = _.get(this.props.product, 'attributes.description.v', '');
    const descriptionList = _.get(this.props.product, 'attributes.description_list.v', '');
    return (
      <div>
        <div
          styleName="description"
          dangerouslySetInnerHTML={{__html: description}}
        />
        <div
          styleName="description-list"
          dangerouslySetInnerHTML={{__html: descriptionList}}
        />
      </div>
    );
  }

  get productForm(): Element<any> {
    if (this.isGiftCard()) {
      return (
        <GiftCardForm
          product={this.product}
          onSkuChange={this.setCurrentSku}
          selectedSku={this.currentSku}
          attributes={this.state.attributes}
          onAttributeChange={this.setAttributeFromField}
        />
      );
    }
    return (
      <ProductDetails
        product={this.product}
        quantity={this.state.quantity}
        onQuantityChange={this.changeQuantity}
      />
    );
  }

  render(): Element<any> {
    const {
      t,
      isLoading,
      notFound,
      fetchError,
      isRelatedProductsLoading,
      relatedProducts,
    } = this.props;

    if (isLoading) {
      return <Loader />;
    }

    if (notFound || this.isArchived) {
      return <p styleName="not-found">{t('Product not found')}</p>;
    }

    if (fetchError) {
      return <ErrorAlerts error={fetchError} />;
    }
    const title = this.isGiftCard() ? t('Gift Card') : this.product.title;

    return (
      <div styleName="container">
        <div styleName="gallery">
          {this.renderGallery()}
        </div>
        <div styleName="details">
          <Breadcrumbs
            routes={this.props.routes}
            params={this.props.params}
            styleName="breadcrumbs"
          />
          <ErrorAlerts error={this.state.error} />
          <h1 styleName="title">{title}</h1>
          {this.productForm}
          <div styleName="cart-actions">
            <AddToCartBtn
              onClick={this.addToCart}
            />
            {/* <SecondaryButton styleName="one-click-checkout">1-click checkout</SecondaryButton> */}
          </div>
          {this.productDetails}
        </div>
        {!_.isEmpty(relatedProducts) && relatedProducts.total ?
          <RelatedProductsList
            title="You might also like"
            list={relatedProducts.result}
            isLoading={isRelatedProductsLoading}
            loadingBehavior={LoadingBehaviors.ShowWrapper}
          />
          : false
        }
      </div>
    );
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(localized(Pdp));
