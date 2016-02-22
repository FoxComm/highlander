import React, { PropTypes } from 'react';
import _ from 'lodash';
import { connect } from 'react-redux';
import { transitionTo } from '../../../route-helpers';
import { autobind } from 'core-decorators';

import TotalsSummary from '../../common/totals';
import OrderLineItems from '../../orders/order-line-items';
import { PrimaryButton } from '../../common/buttons';
import OrderShippingAddress from '../../orders/shipping-address';
import OrderShippingMethod from '../../orders/order-shipping-method';
import Payments from '../../orders/payments';

import { haveType } from '../../../modules/state-helpers';
import * as orderActions from '../../../modules/orders/details';
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

const mapDispatchToProps = {
  ...orderActions,
  ...paymentMethodActions};

@connect(mapStateToProps, mapDispatchToProps)
export default class CustomerCart extends React.Component {

  static contextTypes = {
    history: PropTypes.object.isRequired
  };

  componentWillMount() {
    this.props.fetchCustomerCart(this.props.params.customerId);
  }

  get order() {
    return this.props.order.currentOrder;
  }

  @autobind
  editCart() {
    if (this.order) {
      transitionTo(this.context.history, 'order', {order: this.order.referenceNumber});
    }
  }

  render() {
    const props = this.props;

    if (_.isEmpty(props.order.currentOrder)) {
      return <div className="fc-order-details"></div>;
    } else {
      const order = props.order.currentOrder;

      const {
        itemsStatus,
        shippingAddressStatus,
        shippingMethodStatus,
        paymentMethodStatus
        } = props.order.validations;

      return (
        <div className="fc-customer-cart">
          <div className="_header">
            <div className="fc-subtitle">Cart {order.referenceNumber}</div>
            <div className="fc-customer-cart__edit-btn">
              <PrimaryButton onClick={this.editCart}>Edit Cart</PrimaryButton>
            </div>
          </div>
          <div className="fc-order-details">
            <div className="fc-order-details-body">
              <div className="fc-order-details-main">
                <OrderLineItems readOnly={true} isCart={false} status={itemsStatus} {...props} />
                <OrderShippingAddress readOnly={true} isCart={false} status={shippingAddressStatus} order={order}/>
                <OrderShippingMethod readOnly={true} isCart={false} status={shippingMethodStatus} {...props} />
                <Payments readOnly={true} isCart={false} status={paymentMethodStatus} {...props} />
              </div>
              <div className="fc-order-details-aside">
                <TotalsSummary entity={order} title={order.title}/>
              </div>
            </div>
          </div>
        </div>
      );
    }
  }
}
