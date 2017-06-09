/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { bulkExportBulkAction, renderExportModal, getIdsByProps } from 'modules/bulk-export/helpers';

// actions
import { stateTitles } from 'paragons/order';
import { actions } from 'modules/orders/list';
import { actions as bulkActions } from 'modules/orders/bulk';
import { bulkExport } from 'modules/bulk-export/bulk-export';

// components
import { Link } from 'components/link';
import BulkActions from '../bulk-actions/bulk-actions';
import BulkMessages from '../bulk-actions/bulk-messages';
import { SelectableSearchList } from '../list-page';
import OrderRow from './order-row';
import { ChangeStateModal, CancelModal } from '../bulk-actions/modal';

type Props = {
  list: Object,
  actions: Object,
  bulkActions: {
    cancelOrders: (referenceNumbers: Array<string>, reasonId: number) => Promise<*>,
    changeOrdersState: (referenceNumbers: Array<string>, state: string) => Promise<*>,
    exportByIds: (
      ids: Array<number>, description: string, fields: Array<Object>, entity: string, identifier: string
    ) => void,
  },
  bulkExportAction: (
    fields: Array<string>, entity: string, identifier: string, description: string
  ) => Promise<*>,
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

class Orders extends Component {
  props: Props;

  @autobind
  cancelOrders(allChecked: boolean, toggledIds: Array<string>) {
    const { cancelOrders } = this.props.bulkActions;

    return (
      <CancelModal
        count={toggledIds.length}
        onConfirm={(reasonId) => cancelOrders(toggledIds, reasonId)} />
    );
  }

  @autobind
  bulkExport(allChecked: boolean, toggledIds: Array<string>) {
    const { list } = this.props;
    const { exportByIds } = this.props.bulkActions;
    const modalTitle = 'Orders';
    const entity = 'orders';
    const results = list.currentSearch().results.rows;
    const ids = getIdsByProps('referenceNumber', toggledIds, results);

    return renderExportModal(tableColumns, entity, modalTitle, exportByIds, ids);
  }

  getChangeOrdersState(state: string) {
    const stateTitle = stateTitles[state];

    return (allChecked: boolean, toggledIds: Array<string>) => {
      const {changeOrdersState} = this.props.bulkActions;

      return (
        <ChangeStateModal
          count={toggledIds.length}
          stateTitle={stateTitle}
          onConfirm={() => changeOrdersState(toggledIds, state)} />
      );
    };
  }

  getChangeOrdersStateAction(state: string) {
    const stateTitle = stateTitles[state];

    return [
      `Change Orders state to ${stateTitle}`,
      this.getChangeOrdersState(state),
      `successfully changed state to ${stateTitle}`,
      `could not change state to ${stateTitle}`,
    ];
  }

  get cancelOrdersAction(): Array<any> {
    return [
      'Cancel Selected Orders',
      this.cancelOrders,
      'successfully canceled',
      'could not be canceled',
    ];
  }

  get bulkActions(): Array<any> {
    return [
      bulkExportBulkAction(this.bulkExport, 'Orders'),
      this.cancelOrdersAction,
      this.getChangeOrdersStateAction('manualHold'),
      this.getChangeOrdersStateAction('fraudHold'),
      this.getChangeOrdersStateAction('remorseHold'),
      this.getChangeOrdersStateAction('fulfillmentStarted'),
    ];
  }

  get renderRow(): Function {
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

  renderDetail(message: string, referenceNumber: string) {
    return (
      <span key={referenceNumber}>
        Order <Link to="order-details" params={{order: referenceNumber}}>{referenceNumber}</Link>
        {_.isEmpty(message) ? null : `: ${message}`}
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
          watchActions={true}
          actions={this.bulkActions}>
          <SelectableSearchList
            entity="orders.list"
            exportEntity="orders"
            exportTitle="Orders"
            bulkExport
            bulkExportAction={this.props.bulkExportAction}
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

const mapStateToProps = (state) => {
  return {
    list: _.get(state.orders, 'list', {}),
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
    bulkExportAction: bindActionCreators(bulkExport, dispatch),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(Orders);
