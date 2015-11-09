'use strict';

import React from 'react';
import _ from 'lodash';
import ContentBox from '../content-box/content-box';
import TableView from '../table/tableview';
import Currency from '../common/currency';

const RmaEmail = (props) => {
  if (props.model.storeAdmin) {
    return <span>{props.model.storeAdmin.email}</span>;
  } else if (props.model.customer) {
    return <span>{props.model.customer.email}</span>;
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
    <span>{_.sum(props.model.lineItems.skus, (sku) => sku.totalPrice)}</span>
  );
};

const RmaSummary = (props) => {
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

const RmaList = (props) => {
  return <TableView columns={props.tableColumns} data={{rows: props.items}} />;
};

export {
  RmaEmail,
  CustomerInfo,
  PaymentMethod,
  RmaTotal,
  RmaSummary,
  RmaList
};
