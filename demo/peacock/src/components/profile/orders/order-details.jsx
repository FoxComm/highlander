/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// actions
import { fetchOrder, clearOrder } from 'modules/orders';

// components
import OrderRow from './order-row';
import Loader from 'ui/loader';
import AddressDetails from 'ui/address/address-details';
import Currency from 'ui/currency';
import Icon from 'ui/icon';
import OrderSummary from 'components/order-summary/order-summary';

import styles from '../profile.css';

type Props = {
  referenceNumber: string,
  order: Object,
  fetchOrder: (referenceNumber: string) => Promise<*>,
  fetchOrderState: boolean,
  clearOrder: () => void,
};

class Order extends Component {
  props: Props;

  componentWillMount() {
    const { referenceNumber } = this.props;
    this.props.fetchOrder(referenceNumber);
  }

  componentWillReceiveProps(nextProps: Props) {
    if (this.props.referenceNumber !== nextProps.referenceNumber) {
      this.props.fetchOrder(nextProps.referenceNumber);
    }
  }

  componentWillUnmount() {
    this.props.clearOrder();
  }

  get shippingAddress() {
    const { shippingAddress } = this.props.order;

    return (
      <div styleName="shipping-address">
        <div styleName="title">Shipping address</div>
        <AddressDetails
          address={shippingAddress}
        />
      </div>
    );
  }

  get orderSummary() {
    const { order } = this.props;

    return (
      <OrderSummary
        embedded
        styleName="order-summary"
        {...order}
        skus={order.lineItems.skus}
      />
    );
  }

  get content() {
    const { fetchOrderState } = this.props;

    if (!fetchOrderState) return <Loader />;

    return (
      <div>
        {this.shippingAddress}
        {this.orderSummary}
      </div>
    );
  }

  render() {
    console.log('render() fetchOrderState -> ', this.props.fetchOrderState);
    return (
      <div>
        {this.content}
      </div>
    );
  }
}


  // get shippingAddress() {
  //   const { shippingAddress } = this.props.order;
  //
  //   return <AddressDetails address={shippingAddress} />;
  // }
  //
  // renderCreditCard(paymentMethod) {
  //   return (
  //     <div styleName="payment-method" key={paymentMethod.type}>
  //       <div>
  //         <Icon styleName="payment-icon" name={`fc-payment-${paymentMethod.brand.toLowerCase()}`} />
  //       </div>
  //       <div>
  //         <strong>{paymentMethod.holderName}</strong>
  //       </div>
  //       <div>
  //         XXXX-XXXX-XXXX-{paymentMethod.lastFour}
  //       </div>
  //       <div>
  //         {paymentMethod.expMonth}/{paymentMethod.expYear}
  //       </div>
  //     </div>
  //   );
  // }
  //
  // renderGiftCard(paymentMethod) {
  //   return (
  //     <div styleName="payment-method" key={paymentMethod.code}>
  //       <div>GIFT CARD {paymentMethod.code}</div>
  //       <div>
  //         <strong><Currency value={paymentMethod.amount} /></strong>
  //       </div>
  //     </div>
  //   );
  // }
  //
  // renderStoreCredit(paymentMethod) {
  //   return (
  //     <div styleName="payment-method" key={paymentMethod.type}>
  //       <div>GIFT CARD BALANCE</div>
  //       <div>
  //         <strong><Currency value={paymentMethod.amount} /></strong>
  //       </div>
  //     </div>
  //   );
  // }
  //
  // @autobind
  // renderPaymentMethod(paymentMethod) {
  //   switch (paymentMethod.type) {
  //     case 'creditCard':
  //       return this.renderCreditCard(paymentMethod);
  //     case 'giftCard':
  //       return this.renderGiftCard(paymentMethod);
  //     case 'storeCredit':
  //       return this.renderStoreCredit(paymentMethod);
  //     default:
  //       return null;
  //   }
  // }
  //
  // render() {
  //   const { order } = this.props;
  //   if (!order) {
  //     return <Loader />;
  //   }
  //
  //   return (
  //     <div>
  //       <table styleName="simple-table">
  //         <thead>
  //           <tr>
  //             <th>Date</th>
  //             <th>Order #</th>
  //             <th>Total</th>
  //             <th>Status</th>
  //             <th>Tracking</th>
  //           </tr>
  //         </thead>
  //         <tbody>
  //           <OrderRow order={order} />
  //         </tbody>
  //         <tbody styleName="order-service">
  //           <tr>
  //             <th colSpan="2">SHIPPING</th>
  //             <th colSpan="2">DELIVERY</th>
  //             <th>BILLING</th>
  //           </tr>
  //           <tr>
  //             <td colSpan="2">
  //               {this.shippingAddress}
  //             </td>
  //             <td colSpan="2">
  //               <div styleName="delivery-name">{order.shippingMethod.name}</div>
  //               <div>
  //                 <strong><Currency value={order.shippingMethod.price} /></strong>
  //               </div>
  //             </td>
  //             <td>
  //               {_.map(order.paymentMethods, this.renderPaymentMethod)}
  //             </td>
  //           </tr>
  //         </tbody>
  //       </table>
  //       <OrderSummary
  //         isCollapsed={false}
  //         header={null}
  //         totalTitle="ORDER TOTAL"
  //         embedded
  //         styleName="order-summary"
  //         {...order}
  //         skus={order.lineItems.skus}
  //       />
  //     </div>
  //   );

const mapStateToProps = (state) => {
  return {
    order: _.get(state.orders, 'current', {}),
    fetchOrderState: _.get(state.asyncActions, 'fetchOrder.finished', false),
  };
};

export default connect(mapStateToProps, {
  fetchOrder,
  clearOrder,
})(Order);
