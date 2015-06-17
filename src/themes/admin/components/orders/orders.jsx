'use strict';

import React from 'react';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import OrderStore from './store';
import { listenTo, stopListeningTo } from '../../lib/dispatcher';

export default class Orders extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      orders: OrderStore.getState()
    };
  }

  componentDidMount() {
    listenTo('change', this);
    OrderStore.fetch();
  }

  componentWillUnmount() {
    stopListeningTo('change', this);
  }

  onChange() {
    this.setState({orders: OrderStore.getState()});
  }

  render() {
    return (
      <div id="orders">
        <div className="gutter">
          <table className='listing'>
            <TableHead columns={this.props.tableColumns}/>
            <TableBody columns={this.props.tableColumns} rows={this.state.orders} model='order'/>
          </table>
        </div>
      </div>
    );
  }
}

Orders.propTypes = {
  tableColumns: React.PropTypes.array
};

Orders.defaultProps = {
  tableColumns: [
    {field: 'order', text: 'Order', type: 'id'},
    {field: 'date', text: 'Date', type: 'date'},
    {field: 'email', text: 'Email'},
    {field: 'orderStatus', text: 'Order Status'},
    {field: 'paymentStatus', text: 'Payment Status'},
    {field: 'shippingStatus', text: 'Shipping Status'},
    {field: 'total', text: 'Total', type: 'currency'}
  ]
};
