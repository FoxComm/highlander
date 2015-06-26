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
        <dl className="order-time">
          <dt>{date.format('MM/DD/YYYY')}</dt>
          <dd>{date.format('HH:mm:ss')}</dd>
        </dl>
        <header>Order Summary</header>
        <dl className="order-totals">
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
          <dt><i className="fa fa-user"></i></dt>
          <dd>{customer.id}</dd>
          <dt><i className="fa fa-envelope"></i></dt>
          <dd>{customer.email}</dd>
          <dt><i className="fa fa-phone"></i></dt>
          <dd>{customer.phoneNumber}</dd>
          <dt><i className="fa fa-location-arrow"></i></dt>
          <dd>{customer.location}</dd>
          <dt><i className="fa fa-mobile"></i></dt>
          <dd>{customer.modality}</dd>
          <dt><i className="fa fa-users"></i></dt>
          <dd>{customer.role}</dd>
        </dl>
      </div>
    );
  }
}

OrderSummary.propTypes = {
  order: React.PropTypes.object
};
