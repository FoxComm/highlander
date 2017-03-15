/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
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

// types
import type { HTMLElement } from 'types';
import type { ProductResponse, ProductSlug } from 'modules/product-details';
import type { RelatedProductResponse } from 'modules/cross-sell';

// components
import Gallery from 'ui/gallery/gallery';
import Loader from 'ui/loader';
import ErrorAlerts from '@foxcomm/wings/lib/ui/alerts/error-alerts';
import ProductDetails from './product-details';
import GiftCardForm from '../../components/gift-card-form';
import ProductAttributes from './product-attributes';
import ImagePlaceholder from '../../components/products-item/image-placeholder';

// styles
import styles from './pdp.css';

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

type Props = Localized & {
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
  amountOfServings: string,
  servingSize: string,
  pathName: string,
};

const mapStateToProps = state => {
  const product = state.productDetails.product;
  const relatedProducts = state.crossSell.relatedProducts;

  return {
    product,
    relatedProducts,
    fetchError: _.get(state.asyncActions, 'pdp.err', null),
    notFound: !product && _.get(state.asyncActions, 'pdp.err.response.status') == 404,
    isLoading: _.get(state.asyncActions, ['pdp', 'inProgress'], true),
    isCartLoading: _.get(state.asyncActions, ['cartChange', 'inProgress'], false),
    isRelatedProductsLoading: _.get(state.asyncActions, ['relatedProducts', 'inProgress'], true),
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
  productPromise: Promise;

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
      const { product } = this.props;
      tracking.viewDetails(this.product);
      this.props.actions.fetchRelatedProducts(product.id, 1);
    });
  }

  componentWillUnmount() {
    this.props.actions.resetProduct();
    this.props.actions.clearRelatedProducts();
  }

  componentWillUpdate(nextProps) {
    const id = this.getId(nextProps);

    if (this.productId !== id) {
      this.props.actions.resetProduct();
      this.fetchProduct(nextProps, id);
      this.props.actions.clearRelatedProducts();
      this.props.actions.fetchRelatedProducts(id, 1);
    }
  }

  safeFetch(id) {
    return this.props.actions.fetch(id).catch(_.noop);
  }

  fetchProduct(_props, _productId) {
    const props = _props || this.props;
    const productId = _productId || this.productId;

    if (this.isGiftCard(props)) {
      return searchGiftCards().then(({ result = [] }) => {
        const giftCard = result[0] || {};
        return this.safeFetch(giftCard.productId);
      });
    }
    return this.safeFetch(productId);
  }

  get productId(): ProductSlug {
    return this.getId(this.props);
  }

  get isArchived(): boolean {
    return !!_.get(this.props, ['product', 'archivedAt']);
  }

  getId(props): ProductSlug {
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
    const stateValue = name == 'giftCard.message' ? value.split('\n').join('<br>') : value;
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
      amountOfServings: _.get(attributes, 'Amount of Servings.v', ''),
      servingSize: _.get(attributes, 'Serving Size.v', ''),
      skus: this.sortedSkus,
      pathName: this.props.location.pathname,
    };
  }

  isGiftCard(props) {
    return (props || this.props).route.name === 'gift-cards';
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
      .catch(ex => {
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

  render(): HTMLElement {
    const { t, isLoading, notFound, fetchError, isRelatedProductsLoading } = this.props;

    if (isLoading) {
      return <Loader />;
    }

    if (notFound || this.isArchived) {
      return <p styleName="not-found">{t('Product not found')}</p>;
    }

    if (fetchError) {
      return <ErrorAlerts error={fetchError} />;
    }

    const product = this.product;

    return (
      <div styleName="container">
        <div styleName="gallery">
          {this.renderGallery()}
        </div>
        <div styleName="details">
          <div styleName="details-wrap">
            {this.isGiftCard() ?
              <GiftCardForm
                product={product}
                addToCart={this.addToCart}
                onSkuChange={this.setCurrentSku}
                selectedSku={this.currentSku}
                attributes={this.state.attributes}
                onAttributeChange={this.setAttributeFromField}
              /> :
              <ProductDetails
                product={product}
                quantity={this.state.quantity}
                onQuantityChange={this.changeQuantity}
                addToCart={this.addToCart}
              />}

            <ErrorAlerts error={this.state.error} />
          </div>
        </div>

        {!this.isGiftCard() && <ProductAttributes product={this.props.product} />}
      </div>
    );
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(localized(Pdp));
