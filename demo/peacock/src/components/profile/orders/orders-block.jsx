/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// actions
import * as actions from 'modules/orders';

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
};

class OrdersBlock extends Component {
  props: Props;

  state: State = {
    error: null,
  };

  componentWillMount() {
    if (_.get(this.props.auth, 'jwt')) {
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
        />
      );
    });
  }

  get content() {
    const { props } = this;

    if (this.state.error) {
      return (
        <ErrorAlerts error={this.state.error} />
      );
    }

    return (
      <table styleName="orders-table">
        <tbody>
          {this.renderOrders}
        </tbody>
      </table>
    );
  }

  render() {
    return (
      <div>
        <div styleName="title">My orders</div>
        {this.content}
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    orders: _.get(state.orders, 'list.result', []),
    auth: _.get(state, 'auth', {}),
  };
};

export default connect(mapStateToProps, {
  ...actions,
})(OrdersBlock);
