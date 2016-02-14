// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// data
import { actions } from '../../modules/orders/list';
import * as bulkActions from '../../modules/orders/bulk';

// components
import BulkActions from '../bulk-actions/bulk-actions';
import { SearchableList } from '../list-page';
import OrderRow from './order-row';
import { CancelOrderModal } from './modal';
import { Link } from '../link';


const mapStateToProps = ({orders: {list}}) => ({list});

const mapDispatchToProps = dispatch => {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
  };
};

@connect(mapStateToProps, mapDispatchToProps)
export default class Orders extends React.Component {
  static propTypes = {
    list: PropTypes.object.isRequired,
    actions: PropTypes.objectOf(PropTypes.func).isRequired,
    bulkActions: PropTypes.objectOf(PropTypes.func).isRequired,
  };

  static tableColumns = [
    {field: 'referenceNumber', text: 'Order', model: 'order'},
    {field: 'placedAt', text: 'Date/Time Placed', type: 'datetime'},
    {field: 'customer.name', text: 'Name'},
    {field: 'customer.email', text: 'Email'},
    {field: 'state', text: 'Order State', type: 'state', model: 'order'},
    {field: 'shipping.state', text: 'Shipment State', type: 'state', model: 'shipment'},
    {field: 'grandTotal', text: 'Total', type: 'currency'}
  ];

  @autobind
  cancelOrders(allChecked, toggledIds) {
    const {cancelOrders} = this.props.bulkActions;

    return (
      <CancelOrderModal
        count={toggledIds.length}
        onConfirm={(reasonId) => {
          cancelOrders(toggledIds, reasonId);
        }} />
    );
  }

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

  render() {
    const {list, actions} = this.props;

    return (
      <BulkActions
        module="orders"
        actions={[
          ['Cancel Orders', this.cancelOrders, 'successfully canceled', 'could not be canceled']
        ]}
        entityForms={['order', 'orders']}
        renderDetail={(messages, referenceNumber) => (
          <span key={referenceNumber}>
            Order <Link to="order-details" params={{order: referenceNumber}}>{referenceNumber}</Link>
          </span>
        )}>
        <SearchableList
          emptyResultMessage="No orders found."
          list={list}
          renderRow={this.renderRow}
          tableColumns={Orders.tableColumns}
          searchActions={actions}
          predicate={({referenceNumber}) => referenceNumber} />
      </BulkActions>
    );
  }
}
