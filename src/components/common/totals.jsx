import React from 'react';
import ContentBox from '../content-box/content-box';

const TotalsFooter = props => {
  const { entityType } = props.entity;
  const text = entityType === 'rma' ? 'Refunds Total' : 'Grand Total';

  return (
    <footer classname="fc-content-box-footer is-highlighted">
      <dl className="fc-totals-summary-grand-total">
        <dt>{text}</dt>
        <dd><Currency value={props.entity.total.total} /></dd>
      </dl>
    </footer>
  );
};

const title = entityType => {
  return entityType === 'rma' ? 'Return Summary' : 'Order Summary';
};

const TotalsDiscounts = props => {
  const subTotalWithDiscounts = entity.totals.subTotal;

  if (props.adjustments) {
    return (
      <div>
        <dt>Discounts</dt>
        <dd><Currency value={props.adjustments}/></dd>
        <dt className="fc-totals-summary-new-subtotal">New Subtotal</dt>
        <dd className="fc-totals-summary-new-subtotal"><Currency value={subTotalWithDiscounts}/></dd>
      </div>
    );
  } else {
    return null;
  }
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
          <TotalsDiscounts {...props} adjustments={adjustments} />
          <dt>Shipping</dt>
          <dd><Currency value={rma.totals.shipping}/></dd>
          <dt>Tax</dt>
          <dd><Currency value={rma.totals.taxes}/></dd>
        </dl>
      </article>
    </ContentBox>
  );
};

export default TotalsSummary;
