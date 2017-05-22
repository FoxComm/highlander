/* @flow */

// libs
import React, { Element } from 'react';
import moment from 'moment';
import { stateTitles } from 'paragons/order';

// components
import Currency from 'ui/currency';
import Modal from 'ui/modal/modal';
import CheckoutForm from 'pages/checkout/checkout-form';

import styles from '../profile.css';

type Props = {
  order: Object,
  showDetailsLink: boolean,
  toggleOrderDetails: () => void,
  orderDetailsVisible: boolean,
};

const OrderRow = (props: Props) => {
  const { showDetailsLink, toggleOrderDetails, orderDetailsVisible } = props;

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
        <div styleName="action-link" onClick={toggleOrderDetails}>
          View details
        </div>
      );
    }

    return null;
  };

  const getOrderDetailsModal = () => {
    const action = {
      handler: toggleOrderDetails,
      title: 'Close',
    };

    return (
      <Modal
        show={orderDetailsVisible}
        toggle={toggleOrderDetails}
      >
        <CheckoutForm
          submit={toggleOrderDetails}
          buttonLabel="Got it"
          title="Order details"
          action={action}
        >
        </CheckoutForm>
      </Modal>
    );
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
      <div styleName="order-cell total">
        <Currency
          value={order.grandTotal}
          currency={order.currency}
        />
      </div>
      {getDetailsColumn()}
      {getOrderDetailsModal()}
    </div>
  );
};

export default OrderRow;
