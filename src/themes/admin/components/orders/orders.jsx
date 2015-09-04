'use strict';

import React from 'react';
import TableView from '../tables/tableview';
import OrderStore from './store';

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
        <TableView
          columns={this.props.tableColumns}
          rows={this.state.orders}
          model='order'
          sort={OrderStore.sort.bind(OrderStore)}
        />
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
