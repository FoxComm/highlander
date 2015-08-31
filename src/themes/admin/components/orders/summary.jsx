'use strict';

import React from 'react';
import { formatCurrency } from '../../lib/format';

export default class OrderSummary extends React.Component {
  render() {
    let order = this.props.order;
    let customer = order.customer;
    let discounts = null;

    if (order.totals.adjustments) {
      discounts = (
        <div>
          <dt>Discounts</dt>
          <dd>{formatCurrency(order.totals.adjustments)}</dd>
        </div>
      );
    }

    return (
      <div id="aside-blocks">
        <div className="order-summary fc-contentBox">
          <header>Order Summary</header>
          <article>
            <dl className="order-totals">
              <dt>Subtotal</dt>
              <dd>{formatCurrency(order.totals.subTotal)}</dd>
              {discounts}
              <dt>Shipping</dt>
              <dd>{formatCurrency(order.totals.shipping)}</dd>
              <dt>Tax</dt>
              <dd>{formatCurrency(order.totals.taxes)}</dd>
            </dl>
          </article>
          <div className="highlighted">
            <dl className="grand-total">
              <dt>Grand Total</dt>
              <dd>{formatCurrency(order.totals.total)}</dd>
            </dl>
          </div>
        </div>

        <div className="customer-info fc-contentBox">
          <header>{customer.firstName} {customer.lastName}</header>
          <article>
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
          </article>
        </div>
      </div>
    );
  }
}

OrderSummary.propTypes = {
  order: React.PropTypes.object
};
