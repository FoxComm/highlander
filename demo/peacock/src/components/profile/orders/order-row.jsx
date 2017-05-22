/* @flow */

// libs
import React, { Element } from 'react';
import moment from 'moment';
import { stateTitles } from 'paragons/order';

// components
import Currency from 'ui/currency';
import { Link } from 'react-router';

import styles from '../profile.css';

type Props = {
  order: Object,
  showDetailsLink: boolean,
};

const OrderRow = (props: Props) => {
  const { showDetailsLink } = props;

  const convertOrderData = (orderDetails: Object): Object => {
    if (!orderDetails.grandTotal) {
      return {
        ...orderDetails,
        grandTotal: orderDetails.total,
        currency: 'USD',
        state: orderDetails.orderState,
      };
    }

    return orderDetails;
  };

  const getDetailsColumn = (): Element<*> | null => {
    if (showDetailsLink) {
      return (
        <div styleName="action-link">View details</div>
      );
    }

    return null;
  };

  const order = convertOrderData(props.order);

  return (
    <div styleName="order-row">
      <div styleName="order-cell">
        {`#${order.referenceNumber}`}
      </div>
      <div styleName="order-cell">
        {moment(order.placedAt).format('LLL')}
      </div>
      <div styleName="order-cell">
        Tracking is not available
      </div>
      <div styleName="order-cell">
        {stateTitles[order.state]}
      </div>
      <div styleName="order-cell">
        <Currency
          value={order.grandTotal}
          currency={order.currency}
        />
      </div>
      {getDetailsColumn()}
    </div>
  );
};

export default OrderRow;
