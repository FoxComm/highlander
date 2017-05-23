/* @flow */

// libs
import React, { Element } from 'react';
import moment from 'moment';
import { stateTitles } from 'paragons/order';

// components
import Currency from 'ui/currency';

import styles from '../profile.css';

type Props = {
  order: Object,
  showDetailsLink: boolean,
  handleViewDetails: (order: string) => void,
};

const OrderRow = (props: Props) => {
  const { showDetailsLink, handleViewDetails } = props;

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

  const order = convertOrderData(props.order);

  const getDetailsColumn = (): Element<*> | null => {
    if (showDetailsLink) {
      return (
        <div styleName="action-link" onClick={() => handleViewDetails(order.referenceNumber)}>
          Details
        </div>
      );
    }

    return null;
  };

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
      <div styleName="order-cell total">
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
