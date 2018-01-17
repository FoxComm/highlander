/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// actions
import { fetchOrders } from 'modules/orders';
import { toggleOrderDetails } from 'modules/profile';

// components
import OrderRow from './order-row';
import ErrorAlerts from '@foxcommerce/wings/lib/ui/alerts/error-alerts';
import Modal from 'ui/modal/modal';
import CheckoutForm from 'pages/checkout/checkout-form';
import OrderDetails from './order-details';

import styles from '../profile.css';

type State = {
  error: ?string,
  referenceNumber: ?string,
};

type Props = {
  auth: Object,
  orders: Array<Object>,
  fetchOrders: () => Promise<*>,
  orderDetailsVisible: boolean,
  toggleOrderDetails: () => void,
};

class OrdersBlock extends Component {
  props: Props;

  state: State = {
    error: null,
    referenceNumber: null,
  };

  componentWillMount() {
    const { auth } = this.props;
    if (_.get(auth, 'jwt')) {
      this.props.fetchOrders().catch((ex) => {
        this.setState({
          error: ex.toString(),
        });
      });
    }
  }

  @autobind
  handleViewDetails(referenceNumber: string) {
    this.setState({ referenceNumber }, () => this.props.toggleOrderDetails());
  }

  get renderOrders() {
    const { orders } = this.props;

    return _.map(orders, (order, i) => {
      return (
        <OrderRow
          order={order}
          key={`order-row-${i}`}
          handleViewDetails={this.handleViewDetails}
        />
      );
    });
  }

  get modalContent() {
    const { referenceNumber } = this.state;

    if (referenceNumber == null) {
      return (
        <div>
          Sorry, no details were found.
        </div>
      );
    }

    const action = {
      handler: this.props.toggleOrderDetails,
      title: 'Close',
    };

    return (
      <CheckoutForm
        error={null}
        submit={() => this.props.toggleOrderDetails()}
        buttonLabel="Close"
        title={`Order #${referenceNumber}`}
        action={action}
      >
        <OrderDetails
          referenceNumber={referenceNumber}
        />
      </CheckoutForm>
    );
  }

  get orderdDetailsModal() {
    const { orderDetailsVisible } = this.props;

    return (
      <Modal
        show={orderDetailsVisible}
        toggle={this.props.toggleOrderDetails}
      >
        {this.modalContent}
      </Modal>
    );
  }

  get content() {
    if (this.state.error) {
      return (
        <ErrorAlerts error={this.state.error} />
      );
    }

    return (
      <div styleName="orders-table">
        {this.renderOrders}
      </div>
    );
  }

  get body() {
    const { orders } = this.props;
    if (_.isEmpty(orders)) return null;

    return (
      <div>
        <div styleName="title">My orders</div>
        <div styleName="divider table" />
        {this.content}
        {this.orderdDetailsModal}
      </div>
    );
  }

  render() {
    return (
      this.body
    );
  }
}

const mapStateToProps = (state) => {
  return {
    orders: _.get(state.orders, 'list.result', []),
    auth: _.get(state, 'auth', {}),
    orderDetailsVisible: _.get(state.profile, 'orderDetailsVisible', false),
  };
};

export default connect(mapStateToProps, {
  fetchOrders,
  toggleOrderDetails,
})(OrdersBlock);
