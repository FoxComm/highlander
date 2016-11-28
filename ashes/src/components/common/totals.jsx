import React, { PropTypes } from 'react';
import _ from 'lodash';
import ContentBox from '../content-box/content-box';
import Currency from './currency';

const TotalsFooter = props => {
  const { entityType } = props.entity;
  const text = entityType === 'rma' ? 'Refunds Total' : 'Grand Total';

  return (
    <footer className="fc-content-box-footer is-highlighted">
      <dl className="fc-totals-summary-grand-total">
        <dt>{text}</dt>
        <dd><Currency value={props.entity.totals.total} /></dd>
      </dl>
    </footer>
  );
};

const discounts = totals => {
  const subTotalWithDiscounts = totals.subTotal - totals.adjustments;

  return (
    <div>
      <dt>Discounts</dt>
      <dd><Currency value={totals.adjustments}/></dd>
      <dt className="fc-totals-summary-new-subtotal">New Subtotal</dt>
      <dd className="fc-totals-summary-new-subtotal"><Currency value={subTotalWithDiscounts}/></dd>
    </div>
  );
};

const shipping = totals => {
  return (
    <div>
      <dt>Shipping</dt>
      <dd><Currency value={totals.shipping}/></dd>
    </div>
  );
};

const TotalsSummary = props => {
  const entity = props.entity;
  const totals = entity.totals;
  const title = `${props.title} Summary`;

  return (
    <ContentBox title={title} className="fc-totals-summary" footer={<TotalsFooter {...props} />}>
      <article className="fc-totals-summary-content">
        <dl className="rma-totals">
          <dt>Subtotal</dt>
          <dd><Currency value={totals.subTotal}/></dd>
          {discounts(entity.totals)}
          {shipping(entity.totals)}
          <dt>Tax</dt>
          <dd><Currency value={totals.taxes}/></dd>
        </dl>
      </article>
    </ContentBox>
  );
};

TotalsSummary.propTypes = {
  entity: PropTypes.shape({
    totals: PropTypes.shape({
      subTotal: PropTypes.number.isRequired,
      shipping: PropTypes.number.isRequired,
      taxes: PropTypes.number.isRequired,
      adjustments: PropTypes.number.isRequired,
      total: PropTypes.number.isRequired
    })
  }),
  title: PropTypes.string.isRequired
};

TotalsFooter.propTypes = {
  entity: PropTypes.shape({
    totals: PropTypes.shape({
      subTotal: PropTypes.number.isRequired,
      shipping: PropTypes.number.isRequired,
      taxes: PropTypes.number.isRequired,
      adjustments: PropTypes.number.isRequired,
      total: PropTypes.number.isRequired
    })
  })
};

export default TotalsSummary;
