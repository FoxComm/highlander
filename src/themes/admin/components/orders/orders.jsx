'use strict';

import React from 'react';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import OrderStore from './store';
import TabView from '../tabs/tabs';

export default class Orders extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      orders: OrderStore.getState()
    };
  }

  componentDidMount() {
    OrderStore.listenToEvent('change', this);
    OrderStore.fetch();
  }

  componentWillUnmount() {
    OrderStore.stopListeningToEvent('change', this);
  }

  onChangeOrderStore() {
    this.setState({orders: OrderStore.getState()});
  }

  render() {
    return (
      <div id="orders">
        <div className="fc-grid">
          <div className="fc-col-2-6">
            <h1>Orders - {this.state.orders.length}</h1>
          </div>
          <div className="fc-col-2-6 fc-push-2-6">
            <button className="fc-btn fc-btn-primary"><i className="fa fa-plus"></i> Order</button>
          </div>
        </div>
        <TabView>
          <li>What</li>
          <li>What</li>
        </TabView>
        <table className="fc-table">
          <TableHead columns={this.props.tableColumns}/>
          <TableBody columns={this.props.tableColumns} rows={this.state.orders} model='order' />
        </table>
      </div>
    );
  }
}

Orders.propTypes = {
  tableColumns: React.PropTypes.array
};

Orders.defaultProps = {
  tableColumns: [
    {field: 'referenceNumber', text: 'Order', type: 'id'},
    {field: 'createdAt', text: 'Date', type: 'date'},
    {field: 'email', text: 'Email'},
    {field: 'orderStatus', text: 'Order Status', type: 'orderStatus'},
    {field: 'paymentStatus', text: 'Payment Status'},
    {field: 'total', text: 'Total', type: 'currency'}
  ]
};
