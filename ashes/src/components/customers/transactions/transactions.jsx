/* @flow */

import React, { Component } from 'react';

// libs
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import * as dsl from 'elastic/dsl';
import { bulkExportBulkAction, renderExportModal } from 'modules/bulk-export/helpers';

// actions
import { actions } from 'modules/customers/transactions/transactions';
import { bulkExport } from 'modules/bulk-export/bulk-export';
import { actions as bulkActions } from 'modules/customers/transactions/bulk';

// components
import ListPage from 'components/list-page/list-page';
import OrderTransactionRow from './transaction-row';
import BulkActions from 'components/bulk-actions/bulk-actions';
import BulkMessages from 'components/bulk-actions/bulk-messages';
import { Link } from 'components/link';

type Props = {
  list: Object,
  customer: Object,
  params: Object,
  actions: Object,
  bulkExportAction: (
    fields: Array<string>, entity: string, identifier: string, description: string
  ) => Promise<*>,
  bulkActions: {
    exportByIds: (
      ids: Array<number>, description: string, fields: Array<Object>, entity: string, identifier: string
    ) => void,
  },
};

const tableColumns = [
  {field: 'referenceNumber', text: 'Order', model: 'order'},
  {field: 'placedAt', text: 'Date/Time', type: 'datetime'},
  {field: 'customer.modality', text: 'Modality'},
  {field: 'state', text: 'Order State', type: 'state', model: 'order'},
  {field: 'payment.state', text: 'Payment State'},
  {field: 'assignee', text: 'Assignee'},
  {field: 'grandTotal', text: 'Total', type: 'currency'},
  //{field: 'return', text: 'Return'},
];

class CustomerTransactions extends Component {
  props: Props;

  componentDidMount() {
    this.props.actions.setExtraFilters([
      dsl.nestedTermFilter('customer.id', this.props.customer.id)
    ]);
    this.props.actions.fetch();
  }

  @autobind
  renderRow(row: Object, index: number, columns: Columns, params: Object) {
    const key = `order-${row.referenceNumber}`;

    return (
      <OrderTransactionRow
        key={key}
        order={row}
        columns={columns}
        params={params}
      />
    );
  }

  @autobind
  bulkExport(allChecked: boolean, toggledIds: Array<number>) {
    const { exportByIds } = this.props.bulkActions;
    const modalTitle = 'Customer Transactions';
    const entity = 'orders';

    return renderExportModal(tableColumns, entity, modalTitle, exportByIds, toggledIds);
  }

  get bulkActions(): Array<any> {
    return [
      bulkExportBulkAction(this.bulkExport, 'Customer Transactions'),
    ];
  }

  renderBulkDetails(context: string, orderCode: string) {
    return (
      <span key={orderCode}>
        Customer's transaction with order <Link to="order-details" params={{order: orderCode}}>{orderCode}</Link>
      </span>
    );
  }

  render() {
    const searchOptions = {
      singleSearch: true,
    };

    return (
      <div className="fc-customer-transactions">
        <BulkMessages
          storePath="customers.transactions.bulk"
          module="customers.transactions"
          entity="customer's transaction"
          renderDetail={this.renderBulkDetails}
        />
        <BulkActions
          module="customers.transactions"
          entity="customer's transaction"
          actions={this.bulkActions}
        >
          <ListPage
            exportEntity="orders"
            exportTitle="Customer Transactions"
            bulkExport
            bulkExportAction={this.props.bulkExportAction}
            entity="customers.transactions.list"
            addTitle="Order"
            emptyMessage="No orders found."
            list={this.props.list}
            renderRow={this.renderRow}
            tableColumns={tableColumns}
            searchActions={this.props.actions}
            searchOptions={searchOptions}
            title="Orders"
          />
        </BulkActions>
      </div>
    );
  }
}

const mapStateToProps = (state, props) => {
  return {
    list: _.get(state.customers, 'transactions.list', {}),
    customer: _.get(state.customers.details[props.params.customerId], 'details', {}),
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkExportAction: bindActionCreators(bulkExport, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(CustomerTransactions);
