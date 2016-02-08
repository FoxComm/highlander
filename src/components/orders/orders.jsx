import React, { PropTypes } from 'react';
import { actions } from '../../modules/orders/list';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';

import OrderRow from './order-row';
import { SearchableList } from '../list-page';

const getState = state => ({ list: state.orders.list });

const mapDispatchToProps = dispatch => {
  return { actions: bindActionCreators(actions, dispatch) };
};

const Orders = props => {
  const renderRow = (row, index, columns, params) => {
    const key = `order-${row.referenceNumber}`;
    return (
      <OrderRow key={key}
                order={row}
                columns={columns}
                params={params} />
    );
  };

  const tableColumns = [
    {field: 'referenceNumber', text: 'Order', model: 'order'},
    {field: 'placedAt', text: 'Date/Time Placed', type: 'datetime'},
    {field: 'customer.name', text: 'Name'},
    {field: 'customer.email', text: 'Email'},
    {field: 'state', text: 'Order State', type: 'state', model: 'order'},
    {field: 'shipping.state', text: 'Shipment State', type: 'state', model: 'shipment'},
    {field: 'grandTotal', text: 'Total', type: 'currency'}
  ];

  return (
    <SearchableList
      emptyResultMessage="No orders found."
      list={props.list}
      renderRow={renderRow}
      tableColumns={tableColumns}
      searchActions={props.actions} />
  );
};

Orders.propTypes = {
  list: PropTypes.object.isRequired,
  actions: PropTypes.object.isRequired,
};

export default connect(getState, mapDispatchToProps)(Orders);
