// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// data
import { stateTitles } from '../../paragons/order';
import { actions } from '../../modules/orders/list';
import * as bulkActions from '../../modules/orders/bulk';

// components
import BulkActions from '../bulk-actions/bulk-actions';
import { SearchableList } from '../list-page';
import OrderRow from './order-row';
import { ChangeStateModal, CancelModal } from '../bulk-actions/modal';
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
      <CancelModal
        count={toggledIds.length}
        onConfirm={(reasonId) => {
          cancelOrders(toggledIds, reasonId);
        }} />
    );
  }

  getChangeOrdersState(state) {
    const stateTitle = stateTitles[state];

    return (allChecked, toggledIds) => {
      const {changeOrdersState} = this.props.bulkActions;

      return (
        <ChangeStateModal
          count={toggledIds.length}
          stateTitle={stateTitle}
          onConfirm={() => {
          changeOrdersState(toggledIds, state);
        }} />
      );
    };
  }

  getChangeStateAction(state) {
    const stateTitle = stateTitles[state];

    return [
      `Change Orders state to ${stateTitle}`,
      this.getChangeOrdersState(state),
      `successfully changed state to ${stateTitle}`,
      `could not change state to ${stateTitle}`,
    ];
  }

  get bulkActions() {
    return [
      ['Cancel Orders', this.cancelOrders, 'successfully canceled', 'could not be canceled'],
      this.getChangeStateAction('manualHold'),
      this.getChangeStateAction('fraudHold'),
      this.getChangeStateAction('remorseHold'),
      this.getChangeStateAction('fulfillmentStarted'),
    ];
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
        actions={this.bulkActions}
        entity="order"
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
