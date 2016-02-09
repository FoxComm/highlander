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

class Orders extends React.Component {
  static propTypes = {
    list: PropTypes.object.isRequired,
    actions: PropTypes.object.isRequired,
  };
  
  get renderRow() {
    return (row, index, columns, params) => {
      const key = `order-${row.referenceNumber}`;
      return (
        <OrderRow
          order={row}
          columns={columns}
          key={key}
          params={params} />
      );
    };
  }

  get tableColumns() {
    return [
      {field: 'referenceNumber', text: 'Order', model: 'order'},
      {field: 'placedAt', text: 'Date/Time Placed', type: 'datetime'},
      {field: 'customer.name', text: 'Name'},
      {field: 'customer.email', text: 'Email'},
      {field: 'state', text: 'Order State', type: 'state', model: 'order'},
      {field: 'shipping.state', text: 'Shipment State', type: 'state', model: 'shipment'},
      {field: 'grandTotal', text: 'Total', type: 'currency'}
    ];
  }

  render() {
    return (
      <SearchableList
        emptyResultMessage="No orders found."
        list={this.props.list}
        renderRow={this.renderRow}
        tableColumns={this.tableColumns}
        searchActions={this.props.actions} />
    );
  }
};

Orders.propTypes = {
  list: PropTypes.object.isRequired,
  actions: PropTypes.object.isRequired,
};

export default connect(getState, mapDispatchToProps)(Orders);
