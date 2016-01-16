import React, { PropTypes } from 'react';
import TabListView from '../tabs/tabs';
import TabView from '../tabs/tab';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { actions as ordersActions } from '../../modules/orders/list';
import OrderRow from './order-row';
import _ from 'lodash';

import ListPage from '../list-page/list-page';

const getState = state => ({ list: state.orders.list });
const mapDispatchToProps = dispatch => {
  return { actions: bindActionCreators(ordersActions, dispatch) };
}; 

@connect(getState, mapDispatchToProps)
export default class Orders extends React.Component {
  constructor(props, context) {
    super(props, context);
  }

  get navLinks() {
    return [
      { title: 'Lists', to: 'orders' },
      { title: 'Insights', to: '' },
      { title: 'Activity Trail', to: '' }
    ];
  }

  get renderRow() {
    return (row, index, columns) => {
      const key = `order-${row.referenceNumber}`;
      return <OrderRow order={row} columns={columns} key={key} />;
    };
  }

  get results() {
    const selectedSearch = this.props.list.selectedSearch;
    return this.props.list.savedSearches[selectedSearch].results;
  }

  get tableColumns() {
    return [
      {field: 'referenceNumber', text: 'Order', type: 'text', model: 'order'},
      {field: 'placedAt', text: 'Date/Time Placed', type: 'datetime'},
      {field: 'customer.name', text: 'Name'},
      {field: 'customer.email', text: 'Email'},
      {field: 'status', text: 'Order State', type: 'status', model: 'order'},
      {field: 'shipping.status', text: 'Shipment State', type: 'status', model: 'shipment'},
      {field: 'grandTotal', text: 'Total', type: 'currency'}
    ];
  }

  render() {
    return (
      <ListPage
        addTitle="Order"
        emptyResultMessage="No orders found."
        list={this.props.list}
        navLinks={this.navLinks}
        renderRow={this.renderRow}
        resultCount={this.results.total}
        tableColumns={this.tableColumns}
        searchActions={this.props.actions}
        title="Orders"
        url="orders_search_view/_search" />        
    );
  }
}
