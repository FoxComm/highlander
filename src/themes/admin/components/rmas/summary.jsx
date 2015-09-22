'use strict';

import React from 'react';
import { formatCurrency } from '../../lib/format';

export default class RmaSummary extends React.Component {
  render() {
    let rma = this.props.rma;

    return (
      <div className="fc-rma-summary fc-content-box">
        <header className="header">Return Summary</header>
        <article>
          <dl className="rma-totals">
            <dt>Subtotal</dt>
            <dd>{formatCurrency(rma.totals.subtotal)}</dd>
            <dt>Shipping</dt>
            <dd>{formatCurrency(rma.totals.shipping)}</dd>
            <dt>Tax</dt>
            <dd>{formatCurrency(rma.totals.taxes)}</dd>
          </dl>
        </article>
        <div className="footer">
          <dl className="grand-total">
            <dt>Refunds Total</dt>
            <dd>{formatCurrency(rma.totals.total)}</dd>
          </dl>
        </div>
      </div>
    );
  }
}

RmaSummary.propTypes = {
  rma: React.PropTypes.object
};
