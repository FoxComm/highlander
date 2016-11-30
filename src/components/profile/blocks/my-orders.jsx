// @flow
import _ from 'lodash';
import React, { Component } from 'react';
import styles from '../profile.css';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import ErrorAlerts from 'wings/lib/ui/alerts/error-alerts';

import Block from '../common/block';
import OrderRow from './order-row';

import * as actions from 'modules/orders';

function mapStateToProps(state) {
  return {
    orders: _.get(state.orders, 'list.result', []),
    auth: state.auth,
  };
}

type State = {
  error: ?string;
};

class MyOrders extends Component {
  state: State = {
    error: null,
  };

  componentWillMount() {
    if (_.get(this.props.auth, 'jwt')) {
      this.props.fetchOrders().catch(ex => {
        this.setState({
          error: ex.toString(),
        });
      });
    }
  }

  @autobind
  renderOrder(order) {
    return <OrderRow order={order} showDetailsLink />;
  }

  get content() {
    const { props } = this;

    if (this.state.error) {
      return (
        <ErrorAlerts error={this.state.error} />
      );
    }

    return (
      <table styleName="simple-table">
        <thead>
        <tr>
          <th>Date</th>
          <th>Order #</th>
          <th>Total</th>
          <th>Status</th>
          <th>Tracking</th>
          <th>&nbsp;</th>
        </tr>
        </thead>
        <tbody>
          {_.map(props.orders, this.renderOrder)}
        </tbody>
      </table>
    );
  }

  render() {
    return (
      <Block title="My Orders">
        {this.content}
      </Block>
    );
  }
}

export default connect(mapStateToProps, actions)(MyOrders);
