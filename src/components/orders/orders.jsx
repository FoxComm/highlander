'use strict';

import React from 'react';
import TableView from '../tables/tableview';
import OrderStore from './../../stores/orders';
import TabListView from '../tabs/tabs';
import TabView from '../tabs/tab';
import SectionTitle from '../section-title/section-title';

export default class Orders extends React.Component {
  constructor(props, context) {
    super(props, context);
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

  onChangeOrderStore(orders) {
    this.setState({orders});
  }

  handleAddOrderClick() {
    console.log('Add order clicked');
  }

  render() {
    return (
      <div id="orders">
        <div className="fc-list-header">
          <SectionTitle title="Orders" count={this.state.orders.length} buttonClickHandler={this.handleAddOrderClick }/>
          <div className="fc-grid gutter">
            <div className="fc-col-md-1-1">
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
        <div className="gutter">
          <TableView
            columns={this.props.tableColumns}
            rows={this.state.orders}
            model='order'
            sort={OrderStore.sort.bind(OrderStore)}
            />
        </div>
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
