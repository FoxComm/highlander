'use strict';

import React from 'react';
import { formatCurrency } from '../../lib/format';

export default class ReturnSummary extends React.Component {
  render() {
    let retrn = this.props.return;

    return (
      <div className="fc-return-summary fc-content-box">
        <header className="header">Return Summary</header>
        <article>
          <dl className="return-totals">
            <dt>Subtotal</dt>
            <dd>{formatCurrency(retrn.totals.subtotal)}</dd>
            <dt>Shipping</dt>
            <dd>{formatCurrency(retrn.totals.shipping)}</dd>
            <dt>Tax</dt>
            <dd>{formatCurrency(retrn.totals.taxes)}</dd>
          </dl>
        </article>
        <div className="highlighted">
          <dl className="grand-total">
            <dt>Refunds Total</dt>
            <dd>{formatCurrency(retrn.totals.total)}</dd>
          </dl>
        </div>
      </div>
    );
  }
}

ReturnSummary.propTypes = {
  return: React.PropTypes.object
};
