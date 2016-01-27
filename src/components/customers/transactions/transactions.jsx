import React, { PropTypes } from 'react';
import { actions } from '../../../modules/customers/transactions';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';

import ListPage from '../../list-page/list-page';
import OrderTransactionRow from './order-txn-row';

const getState = state => ({ list: state.customers.transactions });

const mapDispatchToProps = dispatch => {
  return { actions: bindActionCreators(actions, dispatch) };
};

const CustomerTransactions = props => {

  const renderRow = (row, index, columns) => {
    const key = `order-${row.referenceNumber}`;
    return <OrderTransactionRow order={row} columns={columns} key={key} />;
  };

  const tableColumns = [
    {field: 'referenceNumber', text: 'Order', type: 'text', model: 'order'},
    {field: 'placedAt', text: 'Date/Time', type: 'datetime'},
    {field: 'customer.modality', text: 'Modality'},
    {field: 'status', text: 'Order Status', type: 'status', model: 'order'},
    {field: 'payment.status', text: 'Payment Status'},
    {field: 'assignee', text: 'Assignee'},
    {field: 'grandTotal', text: 'Total'},
    //{field: 'return', text: 'Return'},
  ];

  return (
    <ListPage
      addTitle="Order"
      emptyResultMessage="No orders found."
      list={props.list}
      renderRow={renderRow}
      tableColumns={tableColumns}
      searchActions={props.actions}
      title="Orders"
      url="orders_search_view/_search" />
  );
};

export default connect(getState, mapDispatchToProps)(CustomerTransactions);
