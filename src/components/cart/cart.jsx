/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import classNames from 'classnames';
import { connect } from 'react-redux';
import { browserHistory } from 'react-router';
import { autobind } from 'core-decorators';
import * as tracking from 'lib/analytics';

// localization
import localized from 'lib/i18n';

// components
import Currency from 'ui/currency';
import LineItem from './line-item';
import Button from 'ui/buttons';
import Icon from 'ui/icon';
import ErrorAlerts from '@foxcomm/wings/lib/ui/alerts/error-alerts';
import { skuIdentity } from '@foxcomm/wings/lib/paragons/sku';
import { parseError } from '@foxcomm/api-js';
import CouponCode from '../promo-code/promo-code';

// styles
import styles from './cart.css';

// types
import type { Totals } from 'modules/cart';

// actions
import * as actions from 'modules/cart';
import { saveCouponCode, removeCouponCode } from 'modules/checkout';

type Props = {
  fetch: Function,
  deleteLineItem: Function,
  updateLineItemQuantity: Function,
  toggleCart: Function,
  saveCode: Function,
  removeCode: Function,
  skus: Array<any>,
  coupon: ?Object,
  promotion: ?Object,
  totals: Totals,
  user?: ?Object,
  isVisible: boolean,
  t: any,
};

type State = {
  errors?: Array<any>,
};

class Cart extends Component {
  props: Props;

  state: State = {

  };

  componentDidMount() {
    if (this.props.user) {
      this.props.fetch(this.props.user);
    } else {
      this.props.fetch();
    }
  }

  @autobind
  deleteLineItem(sku) {
    tracking.removeFromCart(sku, sku.quantity);
    this.props.deleteLineItem(sku).catch(ex => {
      this.setState({
        errors: parseError(ex),
      });
    });
  }

  @autobind
  updateLineItemQuantity(sku, quantity) {
    const diff = quantity - sku.quantity;
    if (diff > 0) {
      tracking.addToCart(sku, diff);
    } else if (diff < 0) {
      tracking.removeFromCart(sku, -diff);
    }
    this.props.updateLineItemQuantity(sku, quantity).catch(ex => {
      this.setState({
        errors: parseError(ex),
      });
    });
  }

  get lineItems() {
    if (_.isEmpty(this.props.skus)) {
      return (
        <div styleName="empty-cart">
          <p styleName="empty-text">Your cart is empty</p>
        </div>
      );
    }

    return _.map(this.props.skus, sku => {
      return (
        <LineItem
          {...sku}
          deleteLineItem={() => this.deleteLineItem(sku)}
          updateLineItemQuantity={(id, quantity) => this.updateLineItemQuantity(sku, quantity)}
          key={skuIdentity(sku)}
        />
      );
    });
  }

  @autobind
  closeError(error, index) {
    const { errors } = this.state;

    if (!errors || _.isEmpty(this.state.errors)) return;

    errors.splice(index, 1);

    this.setState({
      errors,
    });
  }

  get errorsLine() {
    if (this.state.errors && !_.isEmpty(this.state.errors)) {
      return <ErrorAlerts errors={this.state.errors} closeAction={this.closeError} />;
    }
  }

  @autobind
  onCheckout() {
    browserHistory.push('/checkout');
  }

  render() {
    const {
      t,
      totals,
      coupon,
      toggleCart,
      skus,
      promotion,
      isVisible,
      saveCode,
      removeCode,
    } = this.props;

    const cartClass = classNames({
      'cart-hidden': !isVisible,
      'cart-shown': isVisible,
    });

    const checkoutDisabled = _.size(skus) < 1;

    return (
      <div styleName={cartClass}>
        <div styleName="overlay" onClick={toggleCart}></div>
        <div styleName="cart-box">
          <div styleName="cart-header" onClick={toggleCart}>
            <Icon name="fc-chevron-left" styleName="back-icon"/>
            <div styleName="header-text">{t('KEEP SHOPPING')}</div>
          </div>

          <div styleName="cart-content">
            <div styleName="line-items">
              {this.lineItems}
            </div>

            <div styleName="coupon">
              <CouponCode
                coupon={coupon}
                promotion={promotion}
                discountValue={totals.adjustments}
                saveCode={saveCode}
                removeCode={removeCode}
                disabled={checkoutDisabled}
                placeholder="Coupon Code"
                theme="dark"
              />
            </div>

            <div styleName="cart-subtotal">
              <div styleName="subtotal-title">{t('SUBTOTAL')}</div>
              <div styleName="subtotal-price">
                <Currency value={ totals.subTotal } />
              </div>
            </div>

            {this.errorsLine}
          </div>

          <div styleName="cart-footer">
            <Button onClick={this.onCheckout} disabled={checkoutDisabled} styleName="checkout-button">
              {t('CHECKOUT')}
            </Button>
          </div>
        </div>
      </div>
    );
  }
}

const mapStateToProps = state => ({ ...state.cart, ...state.auth });

export default connect(mapStateToProps, {
  ...actions,
  saveCode: saveCouponCode,
  removeCode: removeCouponCode,
})(localized(Cart));
