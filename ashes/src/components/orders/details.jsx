/* @flow */

// libs
import React, { Component, Element } from 'react';

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

import type { Order } from 'paragons/order';

type Props = {
  details: {
    order: Order,
  },
  entityType: string;
};

export default class OrderDetails extends Component {
  props: Props;

  static defaultProps = {
    entityType: 'orders',
  };

  render() {
    const { details: { order }, entityType } = this.props;

    return (
      <div className="fc-order-details">
        <div className="fc-order-details-body">
          <div className="fc-order-details-main">
            <OrderLineItems order={order} />
            <DiscountsPanel promotion={order.promotion} />
            <OrderShippingAddress isCart={false} order={order} />
            <OrderShippingMethod isCart={false} order={order} />
            <OrderCoupons isCart={false} order={order} />
            <Payments {...this.props} />
          </div>
          <div className="fc-order-details-aside">
            <TotalsSummary entity={order} title={order.title} />
            <CustomerCard customer={order.customer} />
            <Watchers entity={{entityId: order.referenceNumber, entityType}} />
          </div>
        </div>
      </div>
    );
  };
}
