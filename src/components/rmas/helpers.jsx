import React, { PropTypes } from 'react';
import _ from 'lodash';
import ContentBox from '../content-box/content-box';
import TableRow from '../table/row';
import TableCell from '../table/cell';
import { DateTime } from '../common/datetime';
import Currency from '../common/currency';

const RmaEmail = props => {
  if (props.storeAdmin) {
    return <span>{props.storeAdmin.email}</span>;
  } else if (props.model.customer) {
    return <span>{props.customer.email}</span>;
  }

  return null;
};

RmaEmail.propTypes = {
  model: PropTypes.string
};

const CustomerInfo = props => {
  return (
    <div className="fc-rma-summary fc-content-box">
      <header className="fc-content-box-header">Message for Customer</header>
      <article>
        {props.rma.customerMessage}
      </article>
    </div>
  );
};

CustomerInfo.propTypes = {
  rma: PropTypes.object
};

const PaymentMethod = props => {
  const model = props.model;

  return (
    <div className="fc-payment-method">
      <i className={`fc-icon-lg icon-${model.cardType}`}></i>
      <div>
        <div className="fc-strong">{model.cardNumber}</div>
        <div>{model.cardExp}</div>
      </div>
    </div>
  );
};

const RmaTotal = props => {
  return (
    <span><Currency value={_.sum(props.lineItems.skus, (sku) => sku.totalPrice)} /></span>
  );
};

RmaTotal.propTypes = {
  model: PropTypes.string
};

const RmaSummary = props => {
  const rma = props.rma;

  return (
    <ContentBox title="Return Summary" className="fc-rma-summary">
      <article>
        <dl className="rma-totals">
          <dt>Subtotal</dt>
          <dd><Currency value={rma.totals.subtotal}/></dd>
          <dt>Shipping</dt>
          <dd><Currency value={rma.totals.shipping}/></dd>
          <dt>Tax</dt>
          <dd><Currency value={rma.totals.taxes}/></dd>
        </dl>
      </article>
      <footer className="is-highlighted">
        <dl className="grand-total">
          <dt>Refunds Total</dt>
          <dd><Currency value={rma.totals.total}/></dd>
        </dl>
      </footer>
    </ContentBox>
  );
};

const renderRow = (row, index) => {
  return (
    <TableRow key={`${index}`}>
      <TableCell>{row.referenceNumber}</TableCell>
      <TableCell><DateTime value={row.createdAt} /></TableCell>
      <TableCell>{row.orderRefNum}</TableCell>
      <TableCell><RmaEmail {...row} /></TableCell>
      <TableCell>{row.status}</TableCell>
      <TableCell><RmaTotal {...row} /></TableCell>
    </TableRow>
  );
};

export {
  RmaEmail,
  CustomerInfo,
  PaymentMethod,
  RmaTotal,
  RmaSummary,
  renderRow
};
