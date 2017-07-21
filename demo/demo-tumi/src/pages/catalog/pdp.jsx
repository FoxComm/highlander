/* @flow */

// libs
import _ from 'lodash';
import { assoc } from 'sprout-data';
import React, { Component, Element } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import * as tracking from 'lib/analytics';
import { getTaxonValue } from 'paragons/taxons';
import { api as foxApi } from '../../lib/api';
import { SHA1 } from 'crypto-js';

// i18n
import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

// modules
import { searchGiftCards, fetchRecentlyViewed } from 'modules/products';
import { fetch, getNextId, getPreviousId, resetProduct } from 'modules/product-details';
import { addLineItem, toggleCart } from 'modules/cart';
import { fetchRelatedProducts, clearRelatedProducts } from 'modules/cross-sell';
import { fetchInventorySummary } from 'modules/inventory';

// styles
import styles from './pdp.css';

// components
// import { SecondaryButton } from 'ui/buttons';
import { Link } from 'react-router';
import { FormField, Form } from 'ui/forms';
import AddToCartBtn from 'ui/add-to-cart-btn';
import Currency from 'ui/currency';
import ImageGallery from 'react-image-gallery';
import Loader from 'ui/loader';
import ErrorAlerts from 'ui/alerts/error-alerts';
import FeatureSlider from 'components/feature-slider/feature-slider';
import ProductBreadcrumbs from 'components/pdp-breadcrumbs/pdp-breadcrumbs';
import ProductDescription from './pdp/product-description';
import ProductVariants from './product-variants';

import GiftCardForm from 'components/gift-card-form';
import ImagePlaceholder from 'components/products-item/image-placeholder';
import RelatedProductsList, { LoadingBehaviors } from 'components/related-products-list/related-products-list';

// types
import type { ProductResponse } from 'modules/product-details';
import type { Product as ProductListItemResponse } from 'modules/products';
import type { RelatedProductResponse } from 'modules/cross-sell';
import type { RoutesParams } from 'types';
import type { TProductView } from './types';
import type { Sku } from 'types/sku';

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
  fetchRecentlyViewed: Function,
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
  recentlyViewedProducts: Array<ProductResponse>,
  fetchInventoryState: AsyncState,
  isRelatedProductsLoading: boolean,
  isRecentlyViewedProductsLoading: boolean,
};

type State = {
  error?: any,
  currentSku: ?Sku,
  attributes?: Object,
  quantity: string,
};

const mapStateToProps = (state) => {
  const product = state.productDetails.product;
  const relatedProducts = state.crossSell.relatedProducts;
  const recentlyViewedProducts = state.products.viewed;
  const relatedProductsOrder = state.crossSell.relatedProductsOrder;
  const skusSummary = _.get(state.inventory, 'summary', {});

  return {
    product,
    relatedProducts,
    recentlyViewedProducts,
    relatedProductsOrder,
    skusSummary,
    fetchInventoryState: _.get(state.asyncActions, 'fetchInventorySummary', {}),
    fetchError: _.get(state.asyncActions, 'pdp.err', null),
    notFound: !product && _.get(state.asyncActions, 'pdp.err.response.status') == 404,
    isLoading: _.get(state.asyncActions, ['pdp', 'inProgress'], true),
    isCartLoading: _.get(state.asyncActions, ['cartChange', 'inProgress'], false),
    isRelatedProductsLoading: _.get(state.asyncActions, ['relatedProducts', 'inProgress'], false),
    isRecentlyViewedProductsLoading: _.get(state.asyncActions, ['recentlyViewedProducts', 'inProgress'], false),
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
    fetchRecentlyViewed,
    fetchRelatedProducts,
    clearRelatedProducts,
    fetchInventorySummary,
  }, dispatch),
});

class Pdp extends Component {
  props: Props;
  productPromise: Promise<*>;
  _productVariants: ProductVariants;
  containerNode: ?HTMLElement;

  state: State = {
    currentSku: null,
    attributes: {},
    quantity: '1',
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
      tracking.viewDetails(this.productView);

      foxApi.analytics.trackEvent({
        channel: 1,
        subject: 1,
        verb: 'pdp',
        obj: 'product',
        objId: SHA1(`products/${product.id}`).toString(),
        count: 1,
      });

      if (!isRelatedProductsLoading) {
        actions.fetchRelatedProducts(product.id, 1).catch(_.noop);
        actions.fetchRecentlyViewed().catch(_.noop);

      }

      this.updateInStockInfo();
      // TODO: Re-enable this at some point
      /* if (this.containerNode) {
       *   this.containerNode.scrollIntoView();
       * }*/

      this.updatedRecentlyViewed(this.getId(this.props));
    });
  }

  updatedRecentlyViewed(id: number | string) {
    const viewed = this.recentlyViewedSlugs;

    const viewedUpdated = _.uniq([id, ...viewed]).slice(0, 6);
    localStorage.setItem('viewed', JSON.stringify(viewedUpdated));
  }

  updateInStockInfo() {
    const currentSku = this.currentSku;
    if (currentSku) {
      const skuCode = _.get(currentSku, 'attributes.code.v');
      this.props.actions.fetchInventorySummary(skuCode);
    }
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
      this.updatedRecentlyViewed(nextId);
    }
  }

  safeFetch(id) {
    return this.props.actions.fetch(id)
      .then((product) => {
        this.props.actions.fetchRelatedProducts(product.id, 1).catch(_.noop);
        this.props.actions.fetchRecentlyViewed().catch(_.noop);
      })
      .catch(() => {
        const { params } = this.props;
        this.props.actions.fetch(params.productSlug)
          .then((product) => {
            this.props.actions.fetchRelatedProducts(product.id, 1).catch(_.noop);
            this.props.actions.fetchRecentlyViewed().catch(_.noop);
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

  get productId(): string | number {
    return this.getId(this.props);
  }

  get isArchived(): boolean {
    return !!_.get(this.props, ['product', 'archivedAt']);
  }

  get featureTaxons() {
    const taxons = _.get(this.props, 'product.taxons', []);
    const featureTaxonomy = _.find(taxons, (taxonomyEntity) => {
      const taxonomyName = _.get(taxonomyEntity, 'attributes.name.v');
      return taxonomyName === 'features';
    });

    const featureTaxons = _.get(featureTaxonomy, 'taxons', []);
    const parsedFeatures = featureTaxons.reduce((acc, taxon) => {
      const name = _.get(taxon, 'attributes.name.v');

      if (name) {
        acc[name] = {
          description: _.get(taxon, 'attributes.description.v'),
          icon: _.get(taxon, 'attributes.Icon.v'),
          imageUrl: _.get(taxon, ['attributes', 'Image URL', 'v']),
        };
      }

      return acc;
    }, {});

    return parsedFeatures;
  }

  get recentlyViewedSlugs() {
    if (typeof localStorage === 'undefined') {
      return [];
    }

    const viewed = localStorage.getItem('viewed') || [];
    let parsed = [];

    try {
      parsed = JSON.parse(viewed);
    } catch (e) {
      // do nothing
    }

    return parsed;
  }

  @autobind
  getId(props): string | number {
    const slug = props.params.productSlug;

    if (/^\d+$/g.test(slug)) {
      return parseInt(slug, 10);
    }

    return slug;
  }

  get currentSku(): ?Sku {
    return this.state.currentSku || this.sortedSkus[0];
  }

  get sortedSkus() {
    return _.sortBy(
      _.get(this.props, 'product.skus', []),
      'attributes.salePrice.v.value'
    );
  }

  @autobind
  setCurrentSku(currentSku: Sku) {
    this.setState({ currentSku }, () => {
      this.updateInStockInfo();
    });
  }

  @autobind
  setAttributeFromField({ target: { name, value } }) {
    const namePath = ['attributes', ...name.split('.')];
    const stateValue = name === 'giftCard.message' ? value.split('\n').join('<br>') : value;
    this.setState(assoc(this.state, namePath, stateValue));
  }

  get productView(): TProductView {
    const attributes = _.get(this.props.product, 'attributes', {});
    const price = _.get(this.currentSku, 'attributes.salePrice.v', {});
    let images = _.get(this.currentSku, ['albums', 0, 'images'], []);
    if (_.isEmpty(images)) {
      images = _.get(this.props.product, ['albums', 0, 'images'], []);
    }

    const imageUrls = images.map(image => image.src);

    return {
      title: _.get(attributes, 'title.v', ''),
      description: _.get(attributes, 'description.v', ''),
      images: imageUrls,
      currency: _.get(price, 'currency', 'USD'),
      price: _.get(price, 'value', 0),
      skus: this.sortedSkus,
    };
  }

  get productShortDescription(): ?Element<*> {
    const shortDescription = _.get(this.props.product, 'attributes.shortDescription.v');

    if (!shortDescription) return null;

    return (
      <h2 styleName="short-description">{shortDescription}</h2>
    );
  }

  isGiftCardRoute(props = this.props) {
    return props.route.name === 'gift-cards';
  }

  isGiftCard(props = this.props): boolean {
    const tags = _.get(props.product, 'attributes.tags.v', []);
    return tags.indexOf('GIFT-CARD') !== -1;
  }

  @autobind
  addToCart(): void {
    const { actions, product } = this.props;
    const quantity = Number(this.state.quantity);
    const unselectedFacets = this._productVariants.getUnselectedFacets();
    if (unselectedFacets.length) {
      this._productVariants.flashUnselectedFacets(unselectedFacets);
      return;
    }
    const skuCode = _.get(this.currentSku, 'attributes.code.v', '');
    tracking.addToCart(this.productView, quantity);

    foxApi.analytics.trackEvent({
      channel: 1,
      subject: 1,
      verb: 'cart',
      obj: 'product',
      objId: SHA1(`products/${product.id}`).toString(),
      count: quantity,
    });

    actions.addLineItem(skuCode, quantity, this.state.attributes)
      .then(() => {
        actions.toggleCart();
        this.setState({
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
    let images = _.get(this.currentSku, ['albums', 0, 'images'], []);
    if (_.isEmpty(images)) {
      images = _.get(this.props.product, ['albums', 0, 'images'], []);
    }

    if (_.isEmpty(images)) {
      return <ImagePlaceholder largeScreenOnly />;
    }

    // Append parameters that format images appropriately for the PDP.
    const urlAppend = '?hei=642&layer=2&opac=0&layer=3&opac=0';
    const thumbAppend = '?wid=45&hei=45';

    const galleryImages = images.map((image) => {
      return {
        original: `${image.src}${urlAppend}`,
        thumbnail: `${image.src}${thumbAppend}`,
        thumbnailClass: styles.thumbnail,
      };
    });

    return (
      <ImageGallery
        items={galleryImages}
        thumbnailPosition="left"
        showPlayButton={false}
        showBullets={false}
        showFullscreenButton={false}
        showNav={false}
        slideDuration={0}
        showThumbnails
      />
    );
  }

  renderHashtag() {
    const category = this.getTaxonValue('category');
    if (!category) return null;

    const collection = this.getTaxonValue('collection');
    if (!collection) return null;

    const collectionURL = `/c/${category.toLowerCase()}?COLLECTION=${collection.toUpperCase()}`;

    const images = [
      'https://i1.adis.ws/i/tumi/1468829352963531332_878856828?qlt=95&w=640',
      'https://i1.adis.ws/i/tumi/1476574093783787987_1120089935?qlt=95&w=640',
      'https://i1.adis.ws/i/tumi/1451615504044206194_5417602?qlt=95&w=640',
    ];

    const boxes = images.map((image) => {
      return (
        <div key={image}>
          <img styleName="hashtag-image" src={image} />
        </div>
      );
    });

    return (
      <div styleName="hashtag-block">
        <div styleName="hashtag-header">
          <div styleName="hashtag-title">#PerfectingtheJourney</div>
          <div styleName="hashtag-subtitle">
            with the&nbsp;
            <Link styleName="hashtag-link" to={collectionURL}>{collection}</Link>
            &nbsp;Collection
          </div>
        </div>
        <div styleName="hashtag-images">
          {boxes}
        </div>
      </div>
    );
  }

  get productDetails(): ?Element<*> {
    const sku = this.currentSku;
    if (!sku) return;

    return <ProductDescription styleName="product-description" sku={sku} />;
  }

  @autobind
  handleSkuChange(sku: ?Sku) {
    if (sku) {
      this.setCurrentSku(sku);
    }
  }

  get productForm(): Element<any> {
    if (this.isGiftCard()) {
      return (
        <GiftCardForm
          productView={this.productView}
          onSkuChange={this.setCurrentSku}
          selectedSku={this.currentSku}
          attributes={this.state.attributes}
          onAttributeChange={this.setAttributeFromField}
        />
      );
    }
    return (
      <ProductVariants
        ref={(_ref) => {
          this._productVariants = _ref;
        }}
        product={this.props.product}
        productView={this.productView}
        selectedSku={this.currentSku}
        onSkuChange={this.handleSkuChange}
      />
    );
  }

  get relatedProductsList(): ?Element<*> {
    const { relatedProducts, isRelatedProductsLoading, relatedProductsOrder } = this.props;

    if (_.isEmpty(relatedProducts) || relatedProducts.total < 1) return null;

    return (
      <RelatedProductsList
        title="Recommendations"
        list={relatedProducts.result}
        productsOrder={relatedProductsOrder}
        isLoading={isRelatedProductsLoading}
        loadingBehavior={LoadingBehaviors.ShowWrapper}
      />
    );
  }

  get recentlyViewedProductsList(): ?Element<*> {
    const { recentlyViewedProducts, isRecentlyViewedProductsLoading } = this.props;

    if (_.isEmpty(recentlyViewedProducts)) return null;

    let sorted = [];
    _.forEach(this.recentlyViewedSlugs, function (slug) {
      sorted = _.concat(sorted, _.find(recentlyViewedProducts, { slug }));
    });

    const order = _.compact(sorted).map((p: ProductListItemResponse) => p.productId);

    return (
      <RelatedProductsList
        title="Recently Viewed"
        list={recentlyViewedProducts}
        productsOrder={order}
        isLoading={isRecentlyViewedProductsLoading}
        loadingBehavior={LoadingBehaviors.ShowWrapper}
      />
    );
  }

  get productPrice(): ?Element<any> {
    if (this.isGiftCard()) return null;
    const {
      currency,
      price,
      skus,
    } = this.productView;

    const salePrice = _.get(skus[0], 'attributes.salePrice.v.value', 0);
    const retailPrice = _.get(skus[0], 'attributes.retailPrice.v.value', 0);

    if (retailPrice > salePrice) {
      return (
        <div styleName="price">
          <Currency
            styleName="retail-price"
            value={retailPrice}
            currency={currency}
          />
          <Currency
            styleName="on-sale-price"
            value={salePrice}
            currency={currency}
          />
        </div>
      );
    }

    return (
      <div styleName="price">
        <Currency value={price} currency={currency} />
      </div>
    );
  }

  @autobind
  handleQtyChange(event: SyntheticInputEvent) {
    const oldValue = this.state.quantity;
    let { value } = event.target;
    const qty = Number(value);
    if (value == '-') value = oldValue;
    if (value != '' && (isNaN(qty) || qty <= 0)) value = oldValue;
    this.setState({
      quantity: value,
    });
  }

  get quantityField(): Element<*> {
    return (
      <div styleName="quantity-field">
        <div styleName="quantity-header">Qty:</div>
        <FormField>
          <input
            type="text"
            required
            styleName="quantity-input"
            value={this.state.quantity}
            onChange={this.handleQtyChange}
          />
        </FormField>
      </div>
    );
  }

  get instockStatus(): ?Element<*> {
    const currentSku = this.currentSku;
    if (currentSku) {
      const skuCode = _.get(currentSku, 'attributes.code.v');
      const summary = this.props.skusSummary[skuCode];
      if (!this.props.fetchInventoryState.finished) {
        return (
          <div styleName="stock-message">
            <span styleName="stock-icon spin-animation" />
          </div>
        );
      }
      if (!summary) return null;

      let status;
      let tooltipText;
      let postStatus = null;

      if (summary.onHand > 0) {
        status = 'In Stock';
        tooltipText = (`Most orders for In-Stock products begin shipping as soon as your online purchase is completed.\
            Products are shipped once they are located in stock, your payment is approved,\
            and the receiving address is verified.
        `);

        if (summary.onHand < 3) {
          postStatus = `Hurry, ${summary.onHand} left!`;
        }
      } else {
        status = 'OUT OF STOCK';
      }

      return (
        <div styleName="stock-message">
          <span styleName="stock-icon" />
          <div styleName="stock-tooltip">
            {tooltipText}
          </div>
          <span styleName="stock-status">{status}</span>
          <span styleName="stock-post-status">{postStatus}</span>
        </div>
      );
    }
  }

  get stockMessages(): Element<*> {
    return (
      <div styleName="stock-messages">
        {this.instockStatus}
      </div>
    );
  }

  getTaxonValue(name: string): ?string {
    return getTaxonValue(this.props.product, name);
  }

  get collectionLink(): ?Element<*> {
    const collection = this.getTaxonValue('collection');
    if (!collection) return null;

    const collectionLoc = {
      name: 'category',
      params: {
        categoryName: collection,
      },
    };

    return (
      <div styleName="collection">
        <Link styleName="collection-link" to={collectionLoc}>
          {collection}
        </Link>
        <Link styleName="collection-view-entire" to={collectionLoc}>
          View Entire Collection
        </Link>
      </div>
    );
  }

  render(): Element<any> {
    const {
      t,
      isLoading,
      notFound,
      fetchError,
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
    const title = this.isGiftCard() ? t('Gift Card') : this.productView.title;

    // TODO: Move this from being hardcoded to based on product information.
    const category = this.getTaxonValue('category');
    const categoryLink = category ? {
      label: category,
      to: `/c/${category.toLowerCase()}`,
    } : {};

    const productType = this.getTaxonValue('productType');
    const productTypeLink = productType ? {
      label: productType,
      to: `/c/${category.toLowerCase()}?PRODUCTTYPE=${productType.toUpperCase()}`,
    } : {};

    const featureTaxons = this.featureTaxons;

    return (
      <div ref={containerNode => (this.containerNode = containerNode)} styleName="container">
        <Form onSubmit={this.addToCart}>
          <div styleName="header">
            {category && (
              <ProductBreadcrumbs
                category={categoryLink}
                subCategory={productTypeLink}
              />
            )}
          </div>
          <div styleName="body">
            <div styleName="left">
              <div styleName="images">
                <div styleName="dark-overlay" />
                {this.renderGallery()}
              </div>
              {this.renderHashtag()}
            </div>
            <div styleName="right">
              <h1 styleName="title">{title}</h1>
              <div styleName="collection-and-price">
                {this.collectionLink}
                {this.productPrice}
              </div>
              {this.stockMessages}
              <ErrorAlerts error={this.state.error} />
              <div styleName="product-attributes">
                {this.quantityField}
                {this.productForm}
              </div>
              <div styleName="cart-actions">
                <AddToCartBtn
                  type="submit"
                />
                {/* <SecondaryButton styleName="one-click-checkout">1-click checkout</SecondaryButton> */}
              </div>
              {this.productDetails}
            </div>
          </div>
        </Form>
        <div styleName="related">
          <div styleName="body">
            {this.relatedProductsList}
          </div>
        </div>
        {!_.isEmpty(featureTaxons) && (
          <div styleName="body">
            <h2 styleName="subtitle">TUMI Innovation by Design</h2>
            <FeatureSlider features={featureTaxons} />
          </div>
        )}
        <div styleName="related">
          <div styleName="body">
            {this.recentlyViewedProductsList}
          </div>
        </div>
      </div>
    );
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(localized(Pdp));
