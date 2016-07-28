
/* @flow */

import React, { PropTypes, Component, Element } from 'react';
import _ from 'lodash';
import { connect } from 'react-redux';
import { transitionTo } from 'browserHistory';
import { autobind } from 'core-decorators';

import { PrimaryButton } from 'components/common/buttons';
import OrderDetails from 'components/orders/details';
import WaitAnimation from 'components/common/wait-animation';

import * as cartActions from 'modules/carts/details';

type Details = {
  cart: Object,
  isFetching: boolean,
  failed: Object,
};

type Params = {
  customerId: number;
};

type Props = {
  params: Params,
  details: Details,
  fetchCustomerCart: Function,
};

const mapStateToProps = (state) => {
  return {
    details: state.carts.details,
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

  get cart(): Object {
    return this.props.details.cart;
  }

  @autobind
  editCart() {
    if (this.cart) {
      transitionTo('cart', { cart: this.cart.referenceNumber });
    }
  }

  render() {
    const { failed, isFetching, cart } = this.props.details;

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

  get waitAnimation(): Element {
    return <WaitAnimation/>;
  }

  get errorMessage(): Element {
    return <div className="fc-customer__empty-messages">An error occurred. Try again later.</div>;
  }

  get emptyMessage(): Element {
    return <div className="fc-customer__empty-messages">No current cart found for customer.</div>;
  }

  get content(): Element {
    const order = {
      currentOrder: this.cart,
    };

    return (
      <div>
        <div className="_header">
          <div className="fc-subtitle">Cart {this.cart.referenceNumber}</div>
          <div className="fc-customer-cart__edit-btn">
            <PrimaryButton onClick={this.editCart}>Edit Cart</PrimaryButton>
          </div>
        </div>
        <OrderDetails order={order} />
      </div>
    );
  }
}
