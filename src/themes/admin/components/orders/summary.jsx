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
        <dl className="order-customer">
          <dt><i className="icon-user"></i></dt>
          <dd>{customer.id}</dd>
          <dt><i className="icon-mail-alt"></i></dt>
          <dd>{customer.email}</dd>
          <dt><i className="icon-phone"></i></dt>
          <dd>{customer.phone}</dd>
          <dt><i className="icon-location"></i></dt>
          <dd>{customer.city}, {customer.state}</dd>
          <dt><i className="icon-mobile"></i></dt>
          <dd>{customer.modality}</dd>
          <dt><i className="icon-group"></i></dt>
          <dd>{customer.role}</dd>
        </dl>
      </div>
    );
  }
}

OrderSummary.propTypes = {
  order: React.PropTypes.object
};
