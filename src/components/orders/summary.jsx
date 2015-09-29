'use strict';

import React from 'react';
import { formatCurrency } from '../../lib/format';

export default class OrderSummary extends React.Component {
  render() {
    let order = this.props.order;
    let discounts = null;

    const adjustments = order.totals.adjustments || 0;

    const subtotalWithoutDiscounts = order.totals.subTotal - adjustments;
    const subTotalWithDiscounts = order.totals.subTotal;

    if (order.totals.adjustments) {
      discounts = (
        <div>
          <dt>Discounts</dt>
          <dd>{formatCurrency(order.totals.adjustments)}</dd>
          <dt className="fc-order-summary-new-subtotal">New Subtotal</dt>
          <dd className="fc-order-summary-new-subtotal">{formatCurrency(subTotalWithDiscounts)}</dd>
        </div>
      );
    }

    return (
      <div className="fc-order-summary fc-content-box">
        <header>Order Summary</header>
        <article>
          <dl className="order-totals">
            <dt>Subtotal</dt>
            <dd>{formatCurrency(subtotalWithoutDiscounts)}</dd>
            {discounts}
            <dt>Shipping</dt>
            <dd>{formatCurrency(order.totals.shipping)}</dd>
            <dt>Tax</dt>
            <dd>{formatCurrency(order.totals.taxes)}</dd>
          </dl>
        </article>
        <footer className="is-highlighted">
          <dl className="grand-total">
            <dt>Grand Total</dt>
            <dd>{formatCurrency(order.totals.total)}</dd>
          </dl>
        </footer>
      </div>
    );
  }
}

OrderSummary.propTypes = {
  order: React.PropTypes.object
};
