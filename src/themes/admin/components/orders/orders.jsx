'use strict';

import React from 'react';
import TableView from '../tables/tableview';
import OrderStore from './store';
import TabListView from '../tabs/tabs';
import TabView from '../tabs/tab';

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
        <div className="fc-list-header">
          <div className="fc-grid gutter">
            <div className="fc-col-2-6">
              <h1 className="fc-title">Orders <span className="fc-subtitle">{this.state.orders.length}</span></h1>
            </div>
            <div className="fc-col-2-6 fc-push-2-6 fc-actions">
              <button className="fc-btn fc-btn-primary"><i className="fa fa-plus"></i> Order</button>
            </div>
          </div>
          <div className="fc-grid gutter">
            <div className="fc-col-1-1">
              <ul className="fc-tabbed-nav">
                <li><a href="">Lists</a></li>
                <li><a href="">Returns</a></li>
              </ul>
            </div>
          </div>
          <TabListView>
            <TabView>What</TabView>
            <TabView>What</TabView>
          </TabListView>
        </div>
        <TableView
          columns={this.props.tableColumns}
          rows={this.state.orders}
          model='order'
        />
      </div>
    );
  }
}

Orders.propTypes = {
  tableColumns: React.PropTypes.array,
  subNav: React.PropTypes.array
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
