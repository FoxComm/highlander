
/* @flow */

import React, { PropTypes, Component } from 'react';
import _ from 'lodash';
import { connect } from 'react-redux';
import { transitionTo } from 'browserHistory';
import { autobind } from 'core-decorators';

import { PrimaryButton } from 'components/common/buttons';
import OrderDetails from 'components/orders/details';
import WaitAnimation from 'components/common/wait-animation';

import * as cartActions from 'modules/carts/details';

import type { Order } from 'paragons/order';

type Details = {
  cart: Order,
};

type Params = {
  customerId: number;
};

type Props = {
  params: Params,
  details: Details,
  failed: Object,
  isFetching: boolean,
  fetchCustomerCart: Function,
};

const mapStateToProps = (state) => {
  return {
    details: state.carts.details,
    isFetching: _.get(state.asyncActions, 'fetchCart.inProgress', false),
    failed: _.get(state.asyncActions, 'fetchCart.err'),
  };
};

/* ::`*/
@connect(mapStateToProps, cartActions)
/* ::`*/
export default class CustomerCart extends Component {
  props: Props;

  componentWillMount() {
    this.props.fetchCustomerCart(this.props.params.customerId);
  }

  get cart(): Order {
    return this.props.details.cart;
  }

  @autobind
  editCart() {
    if (this.cart) {
      transitionTo('cart', { cart: this.cart.referenceNumber });
    }
  }

  render() {
    const { cart } = this.props.details;
    const { failed, isFetching } = this.props;

    let content;

    if (failed) {
      content = this.errorMessage;
    } else if (isFetching) {
      content = this.waitAnimation;
    } else if (_.isEmpty(cart)) {
      content = this.emptyMessage;
    } else {
      content = this.content;
    }

    return (
      <div className="fc-customer-cart">
        {content}
      </div>
    );
  }

  get waitAnimation() {
    return <WaitAnimation/>;
  }

  get errorMessage() {
    return <div className="fc-customer__empty-messages">An error occurred. Try again later.</div>;
  }

  get emptyMessage() {
    return <div className="fc-customer__empty-messages">No current cart found for customer.</div>;
  }

  get content() {
    const details = {
      order: this.cart,
    };

    return (
      <div>
        <div className="_header">
          <div className="fc-subtitle">Cart {this.cart.referenceNumber}</div>
          <div className="fc-customer-cart__edit-btn">
            <PrimaryButton onClick={this.editCart}>Edit Cart</PrimaryButton>
          </div>
        </div>
        <OrderDetails details={details} entityType="carts" />
      </div>
    );
  }
}
