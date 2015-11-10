'use strict';

import React, { PropTypes } from 'react';
import { formatCurrency } from '../../lib/format';
import ContentBox from '../content-box/content-box';

export default class OrderSummary extends React.Component {
  get footer() {
    let order = this.props.order;
    return (
      <footer className="fc-content-box-footer is-highlighted">
        <dl className="fc-grand-total">
          <dt>Grand Total</dt>
          <dd>{formatCurrency(order.totals.total)}</dd>
        </dl>
      </footer>
    );
  }
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
      <ContentBox title="Order Summary" className="fc-order-summary" footer={this.footer}>
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
      </ContentBox>
    );
  }
}

OrderSummary.propTypes = {
  order: PropTypes.object
};
