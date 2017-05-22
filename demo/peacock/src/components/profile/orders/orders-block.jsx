/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';

// actions
import { fetchOrders } from 'modules/orders';
import { toggleOrderDetails } from 'modules/profile';

// components
import OrderRow from './order-row';
import ErrorAlerts from '@foxcomm/wings/lib/ui/alerts/error-alerts';

import styles from '../profile.css';

type State = {
  error: ?string;
};

type Props = {
  auth: Object,
  orders: Array<Object>,
  fetchOrders: () => Promise<*>,
};

class OrdersBlock extends Component {
  props: Props;

  state: State = {
    error: null,
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

  get renderOrders() {
    const { orders } = this.props;

    return _.map(orders, (order, i) => {
      return (
        <OrderRow
          order={order}
          showDetailsLink
          key={`order-row-${i}`}
          toggleOrderDetails={this.props.toggleOrderDetails}
          orderDetailsVisible={this.props.orderDetailsVisible}
        />
      );
    });
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

  render() {
    return (
      <div>
        <div styleName="title">My orders</div>
        <div styleName="divider table" />
        {this.content}
      </div>
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
