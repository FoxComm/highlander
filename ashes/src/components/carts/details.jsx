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
import Participants from '../participants';

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

  render(): Element {
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
            <CartLineItems status={itemsStatus} cart={cart} />
            <DiscountsPanel promotion={cart.promotion} />
            <CartShippingAddress status={shippingAddressStatus} cart={cart} />
            <CartShippingMethod status={shippingMethodStatus} cart={cart} />
            <CartCoupons cart={cart} />
            <CartPayments cart={cart} status={paymentMethodStatus} />
            <Checkout cart={cart} validations={details.validations} />
          </div>
          <div className="fc-order-details-aside">
            <Messages errors={errors} warnings={warnings} />
            <TotalsSummary entity={cart} title="Cart" />
            <CustomerCard customer={cart.customer} />
            <Participants entity={{entityId: cart.referenceNumber, entityType: 'carts'}} />
          </div>
        </div>
      </div>
    );
  }
}
