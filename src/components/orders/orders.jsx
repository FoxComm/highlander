import React, { PropTypes } from 'react';
import { actions } from '../../modules/orders/list';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';

import ListPage from '../list-page/list-page';
import OrderRow from './order-row';

const getState = state => ({ list: state.orders.list });

const mapDispatchToProps = dispatch => {
  return { actions: bindActionCreators(actions, dispatch) };
};

const Orders = props => {
  const navLinks = [
    { title: 'Lists', to: 'orders' },
    { title: 'Insights', to: '' },
    { title: 'Activity Trail', to: '' }
  ];

  const renderRow = (row, index, columns) => {
    const key = `order-${row.referenceNumber}`;
    return <OrderRow order={row} columns={columns} key={key} />;
  };

  const tableColumns = [
    {field: 'referenceNumber', text: 'Order', type: 'text', model: 'order'},
    {field: 'placedAt', text: 'Date/Time Placed', type: 'datetime'},
    {field: 'customer.name', text: 'Name'},
    {field: 'customer.email', text: 'Email'},
    {field: 'state', text: 'Order State', type: 'status', model: 'order'},
    {field: 'shipping.status', text: 'Shipment State', type: 'status', model: 'shipment'},
    {field: 'grandTotal', text: 'Total', type: 'currency'}
  ];

  return (
    <ListPage
      addTitle="Order"
      emptyResultMessage="No orders found."
      list={props.list}
      navLinks={navLinks}
      renderRow={renderRow}
      tableColumns={tableColumns}
      searchActions={props.actions}
      title="Orders"
      url="orders_search_view/_search" />
  );
};

export default connect(getState, mapDispatchToProps)(Orders);
