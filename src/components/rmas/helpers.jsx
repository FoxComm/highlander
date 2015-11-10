'use strict';

import React from 'react';
import _ from 'lodash';
import { formatCurrency } from '../../lib/format';
import ContentBox from '../content-box/content-box';
import TableRow from '../table/row';
import TableCell from '../table/cell';
import { DateTime } from '../common/datetime';

const RmaEmail = (props) => {
  if (props.storeAdmin) {
    return <span>{props.storeAdmin.email}</span>;
  } else if (props.customer) {
    return <span>{props.customer.email}</span>;
  }

  return null;
};

const CustomerInfo = (props) => {
  return (
    <div className="fc-rma-summary fc-content-box">
      <header className="fc-content-box-header">Message for Customer</header>
      <article>
        {props.rma.customerMessage}
      </article>
    </div>
  );
};

const PaymentMethod = (props) => {
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

const RmaTotal = (props) => {
  return (
    <span>{_.sum(props.lineItems.skus, (sku) => sku.totalPrice)}</span>
  );
};

const RmaSummary = (props) => {
  const rma = props.rma;

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
