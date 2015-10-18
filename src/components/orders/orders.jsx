'use strict';

import React, { PropTypes } from 'react';
import TableView from '../tables/tableview';
import TabListView from '../tabs/tabs';
import TabView from '../tabs/tab';
import SectionTitle from '../section-title/section-title';
import { connect } from 'react-redux';
import * as orderActions from '../../modules/orders';

@connect(state => ({orders: state.orders}), orderActions)
export default class Orders extends React.Component {
  constructor(props, context) {
    super(props, context);
  }

  static propTypes = {
    tableColumns: PropTypes.array,
    subNav: PropTypes.array,
    orders: PropTypes.shape({ items: PropTypes.array }),
    fetchOrdersIfNeeded: PropTypes.func,
  };

  static defaultProps = {
    tableColumns: [
      {field: 'referenceNumber', text: 'Order', type: 'id'},
      {field: 'createdAt', text: 'Date', type: 'date'},
      {field: 'email', text: 'Email'},
      {field: 'orderStatus', text: 'Order Status', type: 'orderStatus'},
      {field: 'paymentStatus', text: 'Payment Status'},
      {field: 'total', text: 'Total', type: 'currency'}
    ]
  };

  componentDidMount() {
    this.props.fetchOrdersIfNeeded();
  }

  handleAddOrderClick() {
    console.log('Add order clicked');
  }

  render() {
    let orders = this.props.orders.items || [];

    return (
      <div id="orders">
        <div className="fc-list-header">
          <SectionTitle title="Orders" count={orders.size} buttonClickHandler={this.handleAddOrderClick }/>
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
            rows={orders}
            model='order'
            sort={this.props.orders.sortColumn}
            />
        </div>
      </div>
    );
  }
}
