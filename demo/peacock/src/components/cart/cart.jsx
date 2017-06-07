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
import { emailIsSet } from 'paragons/auth';
import sanitizeAll from 'sanitizers';

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
import GuestAuth from 'pages/checkout/guest-auth/guest-auth';

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
  location: Object,
};

type State = {
  errors?: Array<any>,
  guestAuth: boolean,
};

class Cart extends Component {
  props: Props;

  state: State = {
    guestAuth: false,
  };

  componentDidMount() {
    this.props.checkApplePay();
    if (this.props.user) {
      this.props.fetch(this.props.user);
    } else {
      this.props.fetch();
    }
  }

  componentWillReceiveProps(nextProps: Props) {
    console.log('receiving props');
    console.log('applePayAvailable from next ->', nextProps.applePayAvailable);
    console.log('applePayAvailable -> ', this.props.applePayAvailable);
    if (this.props.applePayAvailable !== nextProps.applePayAvailable) {
      this.props.checkApplePay();
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

  @autobind
  isEmailSetForCheckout() {
    const user = _.get(this.props, 'user', null);
    return emailIsSet(user);
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

  @autobind
  sanitize(err) {
    if (/Following SKUs are out/.test(err)) {
      const skus = err.split('.')[0].split(':')[1].split(',');

      const products = _.reduce(skus, (acc, outOfStock) => {
        const sku = _.find(this.props.skus, { sku: outOfStock.trim() });
        if (sku) {
          return [
            ...acc,
            sku.name,
          ];
        }

        return acc;
      }, []);

      return (
        <span>
          Products <strong>{products.join(', ')}</strong> are out of stock. Please remove them to complete the checkout.
        </span>
      );
    }

    return sanitizeAll(err);
  }

  get errorsLine() {
    const { applePayStatus } = this.props;
    const { errors }  = this.state;

    if (!errors && _.isEmpty(errors)) return null;

    console.log('errors in total -> ', errors);

    return (
      <ErrorAlerts
        errors={errors}
        sanitizeError={this.sanitize}
      />
     );
  }

  @autobind
  onCheckout() {
    this.setState({ errors: null });
    Promise.resolve(this.props.hideCart())
      .then(() => {
        browserHistory.push('/checkout');
      })
    ;
  }

  @autobind
  beginApplePay() {
    console.log('starting the apple pay inside the checkout.jsx');
    const { total, taxes, adjustments } = this.props.totals;
    console.log('total -> ', total);
    console.log('subTotal -> ', this.props.totals.subTotal);
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

    const lineItems = {
      taxes: taxes,
      promotion: adjustments,
    };
    console.log('payment request obj -> ', paymentRequest);
    console.log('lineItems -> ', lineItems);
    this.props.beginApplePay(paymentRequest, lineItems)
      .then(() => {
        console.log('redirecting to the order confirmation page...');
        this.setState({ errors: null });
        browserHistory.push('/checkout/done')
      })
      .catch((err) => {
        console.log('caught an error in cart.jsx');
        this.setState({
          errors: parseError(err),
        });
      });
  }

  @autobind
  checkAuth() {
    const emailSet = this.isEmailSetForCheckout();

    if (emailSet) {
      this.setState({ guestAuth: false });
      this.beginApplePay();
    } else {
      this.setState({ guestAuth: true });
    }
  }

  get applePayButton() {
    if (!this.props.applePayAvailable) return null;

    const disabled = _.size(this.props.skus) < 1;
    return (
      <Button
        styleName="apple-pay checkout-button"
        onClick={this.checkAuth}
        disabled={disabled}
      />
    );
  }

  get guestAuth() {
    const { guestAuth } = this.state;

    if (!guestAuth) return null;

    return (
      <GuestAuth
        isEditing={!this.isEmailSetForCheckout()}
        location={this.props.location}
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
            {this.errorsLine}
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
        {this.guestAuth}
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    ...state.cart,
    ...state.auth,
    applePayAvailable: _.get(state.checkout, 'applePayAvailable', false),
    location: _.get(state.routing, 'location', {}),
  };
};

export default connect(mapStateToProps, {
  ...actions,
  checkApplePay,
  beginApplePay,
})(localized(Cart));
