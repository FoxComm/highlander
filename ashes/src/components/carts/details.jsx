/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';

// components
import TotalsSummary from '../common/totals';
import Checkout from './checkout';
import CustomerCard from 'components/customer-card/customer-card';
import Messages from './messages';
import CartCoupons from './coupons';
import CartLineItems from './line-items';
import CartPayments from './payments';
import CartShippingAddress from './shipping-address';
import CartShippingMethod from './shipping-method';
import DiscountsPanel from 'components/discounts-panel/discounts-panel';
import ParticipantsPanel from '../participants';

import type { Cart } from 'paragons/order';

type Props = {
  details: {
    cart: Cart,
    validations: {
      errors: Array<string>,
      warnings: Array<string>,
      itemsStatus: string,
      shippingAddressStatus: string,
      shippingMethodStatus: string,
      paymentMethodStatus: string,
    },
  },
};

export default class CartDetails extends Component {
  props: Props;

  render() {
    const { details } = this.props;
    const { cart } = details;

    if (_.isEmpty(cart)) {
      return <div className="fc-order-details"></div>;
    }

    const {
      errors,
      warnings,
      itemsStatus,
      shippingAddressStatus,
      shippingMethodStatus,
      paymentMethodStatus
    } = details.validations;

    return (
      <div className="fc-order-details">
        <div className="fc-order-details-body">
          <div className="fc-order-details-main">
            <CartLineItems id="fct-cart-items-block" status={itemsStatus} cart={cart} />
            <DiscountsPanel id="fct-cart-discounts-block" promotion={cart.promotion} />
            <CartShippingAddress id="fct-cart-shipping-address-block" status={shippingAddressStatus} cart={cart} />
            <CartShippingMethod id="fct-cart-shipping-method-block" status={shippingMethodStatus} cart={cart} />
            <CartCoupons id="fct-cart-coupons-block" cart={cart} />
              <CartPayments id="fct-cart-payment-method-block" cart={cart} status={paymentMethodStatus} />
            <Checkout cart={cart} validations={details.validations} />
          </div>
          <div className="fc-order-details-aside">
            <Messages errors={errors} warnings={warnings} />
            <TotalsSummary entity={cart} title="Cart" />
            <CustomerCard customer={cart.customer} />
            <ParticipantsPanel entity={{entityId: cart.referenceNumber, entityType: 'carts'}} />
          </div>
        </div>
      </div>
    );
  }
}
