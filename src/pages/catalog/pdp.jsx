/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { browserHistory } from 'react-router';

// i18n
import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

// modules
import { fetch as fetchProducts } from 'modules/products';
import { fetch, getNextId, getPreviousId, resetProduct } from 'modules/product-details';
import { addLineItem, toggleCart } from 'modules/cart';

// types
import type { HTMLElement } from 'types';
import type { ProductResponse } from 'modules/product-details';

// components
import Currency from 'ui/currency';
import Gallery from 'ui/gallery/gallery';
import Loader from 'ui/loader';
import ErrorAlerts from 'wings/lib/ui/alerts/error-alerts';
import AddToCartBtn from 'ui/add-to-cart-btn';
import Autocomplete from 'ui/autocomplete';
import Icon from 'ui/icon';

// styles
import styles from './pdp.css';


type Params = {
  productId: string,
};

type Actions = {
  fetchProducts: Function,
  fetch: (id: number) => any,
  getNextId: Function,
  getPreviousId: Function,
  resetProduct: Function,
  addLineItem: Function,
  toggleCart: Function,
};

type Props = Localized & {
  actions: Actions,
  params: Params,
  product: ?ProductResponse,
  auth: any,
  isLoading: boolean,
  isCartLoading: boolean,
  notFound: boolean,
};

type State = {
  quantity: number,
  error?: any,
};

type Product = {
  title: string,
  description: string,
  images: Array<string>,
  currency: string,
  price: number|string,
  amountOfServings: string,
  servingSize: string,
};

const QUANTITY_ITEMS = _.range(1, 1 + 10, 1);

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

const mapDispatchToProps = dispatch => ({
  actions: bindActionCreators({
    fetch,
    getNextId,
    getPreviousId,
    resetProduct,
    addLineItem,
    toggleCart,
    fetchProducts,
  }, dispatch),
});


class Pdp extends Component {
  props: Props;

  state: State = {
    quantity: 1,
  };

  componentWillMount() {
    const {product, actions} = this.props;

    actions.fetchProducts();
    if (!product) {
      actions.fetch(this.productId);
    }
  }

  componentWillUnmount() {
    this.props.actions.resetProduct();
  }

  componentWillUpdate(nextProps) {
    const id = this.getId(nextProps);
    if (this.productId !== id) {
      this.props.actions.fetch(id);
    }
  }

  get productId(): number {
    return this.getId(this.props);
  }

  getId(props: Props): number {
    return parseInt(props.params.productId, 10);
  }

  get firstSku(): Object {
    return _.get(this.props, ['product', 'skus', 0], {});
  }

  get product(): Product {
    const attributes = _.get(this.props.product, 'attributes', {});
    const price = _.get(this.firstSku, 'attributes.salePrice.v', {});
    const images = _.get(this.props.product, ['albums', 0, 'images'], []);
    const imageUrls = images.map(image => image.src);

    return {
      title: _.get(attributes, 'title.v', ''),
      description: _.get(attributes, 'description.v', ''),
      images: imageUrls,
      currency: _.get(price, 'currency', 'USD'),
      price: _.get(price, 'value', 0),
      amountOfServings: _.get(attributes, 'Amount of Servings.v'),
      servingSize: _.get(attributes, 'Serving Size.v'),
    };
  }

  @autobind
  changeQuantity(quantity: number): void {
    this.setState({ quantity });
  }

  @autobind
  addToCart(): void {
    const { actions, auth } = this.props;
    const user = _.get(auth, 'user', null);

    if (_.isEmpty(user)) {
      browserHistory.push({
        pathname: `/products/${this.productId}`,
        query: { auth: 'login' },
      });

      return;
    }

    const { quantity } = this.state;
    const skuId = _.get(this.firstSku, 'attributes.code.v', '');
    actions.addLineItem(skuId, quantity)
      .then(() => {
        actions.toggleCart();
        this.setState({quantity: 1});
      })
      .catch(ex => {
        this.setState({
          error: ex,
        });
      });
  }

  render(): HTMLElement {
    const { t, isLoading, notFound } = this.props;

    if (isLoading) {
      return <Loader/>;
    }

    if (notFound) {
      return <p styleName="not-found">{t('Product not found')}</p>;
    }

    const {
      title,
      description,
      images,
      currency,
      price,
      amountOfServings,
      servingSize,
    } = this.product;

    return (
      <div styleName="container">
        <div styleName="gallery">
          <Gallery images={images} />
        </div>
        <div styleName="details">
          <h1 styleName="title">{title}</h1>
          <div styleName="price">
            <Currency value={price} currency={currency} />
          </div>

          <div styleName="cart-actions">
            <div styleName="quantity">
              <Autocomplete
                inputProps={{
                  type: 'number',
                }}
                getItemValue={_.identity}
                items={QUANTITY_ITEMS}
                onSelect={this.changeQuantity}
                selectedItem={this.state.quantity}
                sortItems={false}
              />
            </div>

            <div styleName="add-to-cart-btn">
              <AddToCartBtn expanded onClick={this.addToCart} />
            </div>
          </div>

          <div
            styleName="description"
            dangerouslySetInnerHTML={{__html: description}}
          />

          <div styleName="servings">
            <div>{amountOfServings}</div>
            <div>{servingSize}</div>
          </div>

          <div styleName="social-sharing">
            <Icon name="fc-instagram" styleName="social-icon"/>
            <Icon name="fc-facebook" styleName="social-icon"/>
            <Icon name="fc-twitter" styleName="social-icon" />
            <Icon name="fc-pinterest" styleName="social-icon"/>
          </div>

          <ErrorAlerts error={this.state.error} />
        </div>
      </div>
    );
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(localized(Pdp));
