import React, { PropTypes } from 'react';
import _ from 'lodash';
import { connect } from 'react-redux';

import TotalsSummary from '../../common/totals';
import OrderLineItems from '../../orders/order-line-items';
import { PrimaryButton } from '../../common/buttons';
import OrderShippingAddress from '../../orders/shipping-address';
import OrderShippingMethod from '../../orders/order-shipping-method';
import Payments from '../../orders/payments';
import Watchers from '../../watchers/watchers';
import { haveType } from '../../../modules/state-helpers';

import * as lineItemActions from '../../../modules/orders/line-items';
import * as skuSearchActions from '../../../modules/orders/sku-search';
import * as orderActions from '../../../modules/orders/details';
import * as shippingMethodActions from '../../../modules/orders/shipping-methods';
import * as skusActions from '../../../modules/skus';
import * as paymentMethodActions from '../../../modules/orders/payment-methods';

const mapStateToProps = (state) => {
  return {
    order: state.orders.details,
    lineItems: state.orders.lineItems,
    skuSearch: state.orders.skuSearch,
    shippingMethods: state.orders.shippingMethods,
    skusActions: state.skusActions,
    payments: state.orders.paymentMethods,
  };
};

const mapDispatchToProps = {...orderActions, ...lineItemActions,
  ...skuSearchActions, ...shippingMethodActions,
  ...skusActions, ...paymentMethodActions};

@connect(mapStateToProps, mapDispatchToProps)
export default class CustomerCart extends React.Component {


  componentWillMount() {
    this.props.fetchCustomerCart(this.props.params.customerId);
  }

  render() {
    const props = this.props;

    if (_.isEmpty(props.order.currentOrder)) {
      return <div className="fc-order-details"></div>;
    } else {
      const order = props.order.currentOrder;
      const isCart = _.isEqual(order.orderState, 'cart');

      const {
        itemsStatus,
        shippingAddressStatus,
        shippingMethodStatus,
        paymentMethodStatus
        } = props.order.validations;

      return (
        <div class="fc-customer-cart">
          <h2>Cart {order.referenceNumber}</h2>
          <PrimaryButton>Edit Cart</PrimaryButton>
          <div className="fc-order-details">
            <div className="fc-order-details-body">
              <div className="fc-order-details-main">
                <OrderLineItems isCart={isCart} status={itemsStatus} {...props} />
                <OrderShippingAddress isCart={isCart} status={shippingAddressStatus} order={order}/>
                <OrderShippingMethod isCart={isCart} status={shippingMethodStatus} {...props} />
                <Payments isCart={isCart} status={paymentMethodStatus} {...props} />
              </div>
              <div className="fc-order-details-aside">
                <TotalsSummary entity={order} title={order.title}/>
                <Watchers entity={haveType(order, 'order')}/>
              </div>
            </div>
          </div>
        </div>
      );
    }
  }
}
