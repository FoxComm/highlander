/* @flow */

// libs
import React, { Component } from 'react';
import { browserHistory } from 'react-router';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { api as foxApi } from 'lib/api';

import localized from 'lib/i18n';

// components
import Button from 'ui/buttons';
import OrderSummary from '../summary/order-summary';

// styles
import styles from './order-placed.css';

// actions
import { fetch as fetchCart } from 'modules/cart';
import { resetCheckout } from 'modules/checkout';

const ORDER = {
  "referenceNumber": "BR11373",
  "paymentState": "auth",
  "lineItems": {
    "skus": [
      {
        "imagePath": "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Marley_Top_Front.jpg",
        "referenceNumber": "9423e657-b61d-808f-290b-55a8c94e2a37",
        "name": "Duckling",
        "sku": "SKU-ZYA",
        "price": 8800,
        "quantity": 1,
        "totalPrice": 8800,
        "productFormId": 17,
        "state": "pending"
      },
      {
        "imagePath": "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Marley_Top_Front.jpg",
        "referenceNumber": "c9ccacf3-39cb-8c76-559b-9d8fbd3b6042",
        "name": "Duckling",
        "sku": "SKU-ZYA",
        "price": 8800,
        "quantity": 1,
        "totalPrice": 8800,
        "productFormId": 17,
        "state": "pending"
      },
      {
        "imagePath": "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Marley_Top_Front.jpg",
        "referenceNumber": "795e376c-e600-e269-5fee-b6dc1fb25b34",
        "name": "Duckling",
        "sku": "SKU-ZYA",
        "price": 8800,
        "quantity": 1,
        "totalPrice": 8800,
        "productFormId": 17,
        "state": "pending"
      }
    ]
  },
  "lineItemAdjustments": [],
  "totals": {
    "subTotal": 26400,
    "taxes": 0,
    "shipping": 3000,
    "adjustments": 0,
    "total": 29400
  },
  "customer": {
    "id": 1224,
    "email": "dark.lord@deathstar.com",
    "name": "Darth Vader",
    "createdAt": "2016-11-03T23:28:32.434Z",
    "disabled": false,
    "isGuest": false,
    "isBlacklisted": false,
    "totalSales": 0
  },
  "shippingMethod": {
    "id": 4,
    "name": "Overnight (FedEx)",
    "code": "OVERNIGHT",
    "price": 3000,
    "isEnabled": true
  },
  "shippingAddress": {
    "id": 1330,
    "region": {
      "id": 4161,
      "countryId": 234,
      "name": "New York"
    },
    "name": "John Doe",
    "address1": "1212 Wide Ave. #333",
    "city": "New York",
    "zip": "10001",
    "isDefault": false,
    "phoneNumber": "9879879876"
  },
  "paymentMethods": [
    {
      "id": 1379,
      "customerId": 1224,
      "holderName": "John Doe",
      "lastFour": "4242",
      "expMonth": 6,
      "expYear": 2019,
      "brand": "Visa",
      "address": {
        "id": 0,
        "region": {
          "id": 4129,
          "countryId": 234,
          "name": "California"
        },
        "name": "John Doe",
        "address1": "1212 Wide Ave. #333",
        "city": "New York",
        "zip": "10001"
      },
      "type": "creditCard"
    }
  ],
  "orderState": "fulfillmentStarted",
  "shippingState": "fulfillmentStarted",
  "fraudScore": 1,
  "placedAt": "2016-11-03T23:39:16.627Z"
};


@connect(state => state.checkout, {fetchCart, resetCheckout})
@localized
class OrderPlaced extends Component {

  static defaultProps = {
    orderPlaced: ORDER.referenceNumber,
  };

  @autobind
  toHome() {
    this.props.fetchCart();
    browserHistory.push('/');
  }

  // get order() {
  //   return foxApi.get(`/v1/my/orders/${this.props.orderPlaced}`)
  // }

  componentWillUnmount() {
    this.props.resetCheckout();
  }

  render() {
    const { t, orderPlaced } = this.props;

    const header = (
      <h2 styleName="subtitle">Your Order</h2>
    );

    const order = ORDER;

    return (
      <div styleName="order-placed">
        <div styleName="thank-you">
          <h1 styleName="title">Thank you for your order!</h1>
          <div styleName="order-number">
            <h2 styleName="subtitle">ORDER CONFIRMATION NUMBER</h2>
            <strong>{orderPlaced}</strong>
          </div>
          <div styleName="desc">
            <p>We’ve received your order and we’re packing up some tasty food!</p>
            <p>Keep your eye out for an email confirmation headed your way shortly.</p>
          </div>
          <Button styleName="to-home" onClick={this.toHome}>{t('Take me home')}</Button>
        </div>

        <OrderSummary
          isCollapsed={false}
          header={header}
          styleName="summary"
          { ...order }
          skus={order.lineItems.skus}
        />
      </div>
    );
  }
}

export default OrderPlaced;
