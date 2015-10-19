'use strict';

import React from 'react';
import { formatCurrency } from '../../lib/format';
import ContentBox from '../content-box/content-box';

export default class RmaSummary extends React.Component {
  render() {
    let rma = this.props.rma;

    return (
      <ContentBox title="Return Summary" className="fc-rma-summary">
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
        <footer className="is-highlighted">
          <dl className="grand-total">
            <dt>Refunds Total</dt>
            <dd>{formatCurrency(rma.totals.total)}</dd>
          </dl>
        </footer>
      </ContentBox>
    );
  }
}

RmaSummary.propTypes = {
  rma: React.PropTypes.object
};
