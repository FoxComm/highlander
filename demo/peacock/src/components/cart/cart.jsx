/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import classNames from 'classnames';
import { connect } from 'react-redux';
import { browserHistory } from 'lib/history';
import { autobind } from 'core-decorators';
import * as tracking from 'lib/analytics';
import localized from 'lib/i18n';

// actions
import * as actions from 'modules/cart';
import { checkApplePay, beginApplePay } from 'modules/checkout';

// components
import Currency from 'ui/currency';
import LineItem from './line-item';
import Button from 'ui/buttons';
import ErrorAlerts from '@foxcomm/wings/lib/ui/alerts/error-alerts';
import { skuIdentity } from '@foxcomm/wings/lib/paragons/sku';
import { parseError } from '@foxcomm/api-js';
import Overlay from 'ui/overlay/overlay';
import ActionLink from 'ui/action-link/action-link';

// types
import type { Totals } from 'modules/cart';

// styles
import styles from './cart.css';

type Props = {
  fetch: Function, // signature
  deleteLineItem: Function, // siganture
  updateLineItemQuantity: Function, // signature
  toggleCart: Function, // signature
  hideCart: Function, // signature
  skus: Array<mixed>,
  coupon: ?Object,
  promotion: ?Object,
  totals: Totals,
  user?: ?Object,
  isVisible: boolean,
  t: any,
  applePayAvailable: boolean,
  checkApplePay: Function, // signature
};

type State = {
  errors?: Array<any>,
};

class Cart extends Component {
  props: Props;

  state: State = {

  };

  componentDidMount() {
    this.props.checkApplePay();
    if (this.props.user) {
      this.props.fetch(this.props.user);
    } else {
      this.props.fetch();
    }
  }

  @autobind
  deleteLineItem(sku) {
    tracking.removeFromCart(sku, sku.quantity);
    this.props.deleteLineItem(sku).catch((ex) => {
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
    this.props.updateLineItemQuantity(sku, quantity).catch((ex) => {
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

    return _.map(this.props.skus, (sku) => {
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
    Promise.resolve(this.props.hideCart())
      .then(() => {
        browserHistory.push('/checkout');
      })
    ;
  }

  @autobind
  beginApplePay() {
    console.log('starting the apple pay inside the checkout.jsx');
    const total = this.props.totals.total;
    console.log('total -> ', total);
    console.log('subTotal -> ', this.props.totals.subTotal);
    console.log('the total from the cart -> ', total);
    const amount = (parseFloat(total)/100).toFixed(2);
    console.log('amount -> ', amount);
    const paymentRequest = {
      countryCode: 'US',
      currencyCode: 'USD',
      total: {
        label: 'Pure',
        amount: amount.toString(),
      },
      'requiredShippingContactFields': [
        'postalAddress',
        'name',
        'phone',
      ],
      'requiredBillingContactFields': [
        'postalAddress',
        'name',
      ],
    };
    console.log('payment request obj -> ', paymentRequest);
    this.props.beginApplePay(paymentRequest).then(() => {
      console.log('redirecting to the order confirmation page...');
       browserHistory.push('/checkout/done')
     });
  }

  get applePayButton() {
    if (!this.props.applePayAvailable) return null;

    return (
      <Button
        styleName="apple-pay checkout-button"
        onClick={this.beginApplePay}
      />
    );
  }

  render() {
    const {
      t,
      totals,
      toggleCart,
      skus,
      isVisible,
      applePayAvailable,
    } = this.props;

    const cartClass = classNames({
      'cart-hidden': !isVisible,
      'cart-shown': isVisible,
    });

    const checkoutDisabled = _.size(skus) < 1;
    const footerClasses = classNames(styles['cart-footer'], {
      [styles['with-apple-pay']]: applePayAvailable,
    });

    const contentClasses = classNames(styles['cart-content'], {
      [styles['with-apple-pay']]: applePayAvailable,
    });

    return (
      <div styleName={cartClass}>
        <Overlay onClick={toggleCart} shown={isVisible} />
        <div styleName="cart-box">
          <div styleName="cart-header">
            <span styleName="my-cart">My Cart</span>
            <ActionLink
              action={toggleCart}
              title="Close"
              styleName="action-link-cart-close"
            />
          </div>

          <div className={contentClasses}>
            <div styleName="line-items">
              {this.lineItems}
            </div>
            {this.errorsLine}
          </div>

          <div className={footerClasses}>
            <Button onClick={this.onCheckout} disabled={checkoutDisabled} styleName="checkout-button">
              <span>{t('Checkout')}</span>
              <span styleName="subtotal-price">
                <Currency value={totals.subTotal} />
              </span>
            </Button>
            {this.applePayButton}
          </div>
        </div>
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    ...state.cart,
    ...state.auth,
    applePayAvailable: _.get(state.checkout, 'applePayAvailable', false),
  };
};

export default connect(mapStateToProps, {
  ...actions,
  checkApplePay,
  beginApplePay,
})(localized(Cart));
