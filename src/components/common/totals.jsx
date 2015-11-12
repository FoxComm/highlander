import React, { PropTypes } from 'react';
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

TotalsFooter.propTypes = {
  entity: PropTypes.shape({
    totals: PropTypes.shape({
      total: PropTypes.number.isRequired
    })
  })
};

const title = entityType => {
  return entityType === 'rma' ? 'Return Summary' : 'Order Summary';
};

const discounts = (adjustments, totals) => {
  if (!adjustments) return null;

  const subTotalWithDiscounts = totals.subTotal;

  return (
    <div>
      <dt>Discounts</dt>
      <dd><Currency value={adjustments}/></dd>
      <dt className="fc-totals-summary-new-subtotal">New Subtotal</dt>
      <dd className="fc-totals-summary-new-subtotal"><Currency value={subTotalWithDiscounts}/></dd>
    </div>
  );
};

const shipping = totals => {
  if (!totals.shipping) return null;

  return (
    <div>
      <dt>Shipping</dt>
      <dd><Currency value={totals.shipping}/></dd>
    </div>
  );
};

const TotalsSummary = props => {
  const entity = props.entity;
  const adjustments = entity.totals.adjustments || 0;
  const subtotalWithoutDiscounts = entity.totals.subTotal - adjustments;

  return (
    <ContentBox title={title(entity.entityType)} className="fc-totals-summary" footer={<TotalsFooter {...props} />}>
      <article className="fc-totals-summary-content">
        <dl className="rma-totals">
          <dt>Subtotal</dt>
          <dd><Currency value={subtotalWithoutDiscounts}/></dd>
          {discounts(adjustments, entity.totals)}
          {shipping(entity.totals)}
          <dt>Tax</dt>
          <dd><Currency value={entity.totals.taxes}/></dd>
        </dl>
      </article>
    </ContentBox>
  );
};

export default TotalsSummary;
