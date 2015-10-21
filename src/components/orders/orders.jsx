'use strict';

import React, { PropTypes } from 'react';
import TableView from '../tables/tableview';
import TabListView from '../tabs/tabs';
import TabView from '../tabs/tab';
import SectionTitle from '../section-title/section-title';
import { connect } from 'react-redux';
import * as orderActions from '../../modules/orders/list';
import LocalNav from '../local-nav/local-nav';

@connect(state => ({orders: state.orders.list}), orderActions)
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
        <div>
          <SectionTitle title="Orders" subtitle={orders.size} buttonClickHandler={this.handleAddOrderClick }/>
          <LocalNav>
            <a href="">Lists</a>
            <a href="">Returns</a>
          </LocalNav>
          <TabListView>
            <TabView>What</TabView>
            <TabView>What</TabView>
          </TabListView>
        </div>
        <div>
          <TableView
            columns={this.props.tableColumns}
            rows={orders}
            model='order'
            />
        </div>
      </div>
    );
  }
}
