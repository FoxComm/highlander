// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// data
import { stateTitles } from '../../paragons/order';
import { groups } from '../../paragons/watcher';
import { actions } from '../../modules/orders/list';
import { actions as bulkActions } from '../../modules/orders/bulk';

// helpers
import { numberize } from '../../lib/text-utils';

// components
import BulkActions from '../bulk-actions/bulk-actions';
import BulkMessages from '../bulk-actions/bulk-messages';
import { SelectableSearchList } from '../list-page';
import OrderRow from './order-row';
import { ChangeStateModal, CancelModal, SelectUsersModal } from '../bulk-actions/modal';
import { Link } from '../link';


const mapStateToProps = ({orders: {list, watchers}}) => {
  return {
    list,
    selectedWatchers: _.get(watchers, 'list.selectModal.selected', []).map(({id}) => id),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
  };
};

const tableColumns = [
  {field: 'referenceNumber', text: 'Order', model: 'order'},
  {field: 'placedAt', text: 'Date/Time Placed', type: 'datetime'},
  {field: 'customer.name', text: 'Customer Name'},
  {field: 'customer.email', text: 'Customer Email'},
  {field: 'state', text: 'Order State', type: 'state', model: 'order'},
  {field: 'shipping.state', text: 'Shipment State', type: 'state', model: 'shipment'},
  {field: 'grandTotal', text: 'Total', type: 'currency'}
];

@connect(mapStateToProps, mapDispatchToProps)
export default class Orders extends React.Component {
  static propTypes = {
    list: PropTypes.object.isRequired,
    selectedWatchers: PropTypes.array.isRequired,
    actions: PropTypes.objectOf(PropTypes.func).isRequired,
    bulkActions: PropTypes.objectOf(PropTypes.func).isRequired,
  };

  @autobind
  cancelOrders(allChecked, toggledIds) {
    const {cancelOrders} = this.props.bulkActions;

    return (
      <CancelModal
        count={toggledIds.length}
        onConfirm={(reasonId) => cancelOrders(toggledIds, reasonId)} />
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
          onConfirm={() => changeOrdersState(toggledIds, state)} />
      );
    };
  }

  getChangeOrdersStateAction(state) {
    const stateTitle = stateTitles[state];

    return [
      `Change Orders state to ${stateTitle}`,
      this.getChangeOrdersState(state),
      `successfully changed state to ${stateTitle}`,
      `could not change state to ${stateTitle}`,
    ];
  }

  getWatchOrders(group, action, isDirectAction) {
    return (allChecked, toggledIds) => {
      const {bulkActions} = this.props;
      const count = toggledIds.length;
      let label = null;

      if (isDirectAction && group === 'assignees') {
        label = 'Assign';
      } else if (!isDirectAction && group === 'assignees') {
        label = 'Unassign';
      } else if (isDirectAction && group === 'watchers') {
        label = 'Watchers for';
      } else if (!isDirectAction && group === 'watchers') {
        label = 'Remove watchers for';
      }

      const bulkAction = isDirectAction ? bulkActions.watchOrders : bulkActions.unwatchOrders;

      return (
        <SelectUsersModal
          action={action}
          count={count}
          labelPrefix={label}
          maxUsers={1}
          onConfirm={() => bulkAction(group, toggledIds, this.props.selectedWatchers)} />
      );
    };
  }

  getWatchOrdersAction(group, action, isDirectAction, successMessage, errorMessage) {
    const actionForm = isDirectAction ? action : `un${action}`;

    return [
      `${_.capitalize(actionForm)} Orders`,
      this.getWatchOrders(group, actionForm, isDirectAction),
      successMessage,
      errorMessage,
    ];
  }

  get bulkActions() {
    return [
      ['Cancel Orders', this.cancelOrders, 'successfully canceled', 'could not be canceled'],
      this.getChangeOrdersStateAction('manualHold'),
      this.getChangeOrdersStateAction('fraudHold'),
      this.getChangeOrdersStateAction('remorseHold'),
      this.getChangeOrdersStateAction('fulfillmentStarted'),
      this.getWatchOrdersAction(
        groups.assignees, 'assign', true, 'successfully assigned', 'failed to assign'
      ),
      this.getWatchOrdersAction(
        groups.assignees, 'assign', false, 'successfully unassigned', 'failed to unassign'
      ),
      this.getWatchOrdersAction(
        groups.watchers, 'watch', true, 'successfully started watching', 'failed to start watching'
      ),
      this.getWatchOrdersAction(
        groups.watchers, 'watch', false, 'failed to stop watching', 'failed to stop watching'
      ),
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

  renderDetail(messages, referenceNumber) {
    return (
      <span key={referenceNumber}>
        Order <Link to="order-details" params={{order: referenceNumber}}>{referenceNumber}</Link>: {messages}
      </span>
    );
  }

  render() {
    const {list, actions} = this.props;

    return (
      <div>
        <BulkMessages
          storePath="orders.bulk"
          module="orders"
          entity="order"
          renderDetail={this.renderDetail} />
        <BulkActions
          module="orders"
          entity="order"
          actions={this.bulkActions}>
          <SelectableSearchList
            emptyMessage="No orders found."
            list={list}
            renderRow={this.renderRow}
            tableColumns={tableColumns}
            searchActions={actions}
            predicate={({referenceNumber}) => referenceNumber} />
        </BulkActions>
      </div>
    );
  }
}
