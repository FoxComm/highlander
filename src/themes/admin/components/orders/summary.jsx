'use strict';

import React from 'react';
import moment from 'moment';
import { formatCurrency } from '../../lib/format';

export default class OrderSummary extends React.Component {
  render() {
    let
      order = this.props.order,
      date  = moment(order.createdAt);

    return (
      <div id="order-summary">
        <header>Order Summary</header>
        <dl>
          <dt>Date</dt>
          <dd>{date.format('MM/DD/YYYY')}</dd>
          <dt>Time</dt>
          <dd>{date.format('h:mm a')}</dd>
          <dt>Subtotal</dt>
          <dd>{formatCurrency(order.subtotal)}</dd>
          <dt>Discounts</dt>
          <dd>{formatCurrency(order.discountTotal)}</dd>
          <dt>Shipping</dt>
          <dd>{formatCurrency(order.shippingTotal)}</dd>
          <dt>Tax</dt>
          <dd>{formatCurrency(order.tax)}</dd>
          <dt>Grand Total</dt>
          <dd>{formatCurrency(order.grandTotal)}</dd>
        </dl>
      </div>
    );
  }
}

OrderSummary.propTypes = {
  order: React.PropTypes.object
};
