'use strict';

import React from 'react';
import moment from 'moment';
import { formatCurrency } from '../../lib/format';

export default class OrderSummary extends React.Component {
  render() {
    let
      order     = this.props.order,
      customer  = order.customer,
      date      = moment(order.createdAt);

    return (
      <div id="order-summary">
        <header>Order Summary</header>
        <dl className="order-totals">
          <dt>Date</dt>
          <dd>{date.format('MM/DD/YYYY')}</dd>
          <dt>Time</dt>
          <dd>{date.format('h:mm a')}</dd>
          <dt>Subtotal</dt>
          <dd>{formatCurrency(order.totals.subTotal)}</dd>
          <dt>Discounts</dt>
          <dd>{formatCurrency(order.totals.adjustments)}</dd>
          <dt>Shipping</dt>
          <dd>{formatCurrency(order.totals.shipping)}</dd>
          <dt>Tax</dt>
          <dd>{formatCurrency(order.totals.taxes)}</dd>
          <dt className="grand-total">Grand Total</dt>
          <dd className="grand-total">{formatCurrency(order.totals.total)}</dd>
        </dl>
        <header>{customer.firstName} {customer.lastName}</header>
        <dl>
          <dt></dt>
          <dd>{customer.id}</dd>
        </dl>
      </div>
    );
  }
}

OrderSummary.propTypes = {
  order: React.PropTypes.object
};
