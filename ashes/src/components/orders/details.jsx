/* @flow */

// libs
import React, { Element } from 'react';

// components
import TotalsSummary from 'components/common/totals';
import CustomerCard from 'components/customer-card/customer-card';
import OrderLineItems from './order-line-items';
import OrderShippingAddress from './shipping-address';
import OrderShippingMethod from './order-shipping-method';
import Payments from './payments';
import DiscountsPanel from 'components/discounts-panel/discounts-panel';
import OrderCoupons from './order-coupons';
import Watchers from '../watchers/watchers';

// helpers
import { getClaims, isPermitted } from 'lib/claims';
import { frn, readAction } from 'lib/frn';

import type { Order } from 'paragons/order';

type Props = {
  details: {
    order: Order,
  },
};

const transactionClaims = readAction(frn.user.customerTransaction);
const adminClaims = readAction(frn.settings.user);

const OrderDetails = (props: Props): Element => {
  const { order } = props.details;
  const actualClaims = getClaims();

  const payments = isPermitted(transactionClaims, actualClaims)
    ? <Payments {...props} />
    : null;

  const watchers = isPermitted(adminClaims, actualClaims)
    ? <Watchers entity={{entityId: order.referenceNumber, entityType: 'orders'}} />
    : null;

  return (
    <div className="fc-order-details">
      <div className="fc-order-details-body">
        <div className="fc-order-details-main">
          <OrderLineItems order={order} />
          <DiscountsPanel promotion={order.promotion} />
          <OrderShippingAddress isCart={false} order={order} />
          <OrderShippingMethod isCart={false} order={order} />
          <OrderCoupons isCart={false} order={order} />
          {payments}
        </div>
        <div className="fc-order-details-aside">
          <TotalsSummary entity={order} title={order.title} />
          <CustomerCard customer={order.customer} />
          {watchers}
        </div>
      </div>
    </div>
  );
};

export default OrderDetails;
