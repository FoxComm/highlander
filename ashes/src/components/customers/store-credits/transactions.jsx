/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { bulkExportBulkAction, renderExportModal } from 'modules/bulk-export/helpers';
import { autobind } from 'core-decorators';

// actions
import { actions as StoreCreditTransactionsActions } from 'modules/customers/store-credit-transactions/transactions';
import * as StoreCreditTotalsActions from 'modules/customers/store-credit-totals';
import { bulkExport } from 'modules/bulk-export/bulk-export';
import { actions as bulkActions } from 'modules/customers/store-credit-transactions/bulk';

// components
import Summary from './summary';
import SelectableSearchList from 'components/list-page/selectable-search-list';
import StoreCreditTransactionRow from './transactions-row';
import BulkActions from 'components/bulk-actions/bulk-actions';
import BulkMessages from 'components/bulk-actions/bulk-messages';
import { Link } from 'components/link';

import styles from './store-credits.css';

type Actions = {
  fetchTotals: Function,
};

type Props = {
  params: Object,
  tableColumns: Array<any>,
  totalsActions: Actions,
  list: Object,
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

class StoreCreditTransactions extends Component {
  props: Props;

  static defaultProps = {
    tableColumns: [
      {
        field: 'createdAt',
        text: 'Date/Time',
        type: 'date'
      },
      {
        field: 'transaction',
        text: 'Transaction',
      },
      {
        field: 'debit',
        text: 'Amount',
        type: 'transaction'
      },
      {
        field: 'state',
        text: 'Payment State',
      },
      {
        field: 'availableBalance',
        text: 'Total Available Balance',
        type: 'currency'
      }
    ]
  };

  get customerId() {
    return this.props.params.customerId;
  }

  componentDidMount() {
    this.props.actions.setExtraFilters([
      { term: { accountId: this.customerId } }
    ]);
    this.props.totalsActions.fetchTotals(this.customerId);
    this.props.actions.fetch();
  }

  renderRow(row: Object, index: number, columns: Columns, params: Object) {
    const key = `sc-transaction-${row.id}`;

    return (
      <StoreCreditTransactionRow
        key={key}
        storeCreditTransaction={row}
        columns={columns}
        params={params}
      />
    );
  }

  @autobind
  bulkExport(allChecked: boolean, toggledIds: Array<number>) {
    const { exportByIds } = this.props.bulkActions;
    const { tableColumns } = this.props;
    const modalTitle = 'Store Credit Transactions';
    const entity = 'storeCreditTransactions';

    return renderExportModal(tableColumns, entity, modalTitle, exportByIds, toggledIds);
  }

  get bulkActions() {
    return [
      bulkExportBulkAction(this.bulkExport, 'Store Credit Transactions'),
    ];
  }

  renderDetail(message: string, orderCode: string) {
    return (
      <span key={orderCode}>
        Store Credit Transaction for order <Link to="order-details" params={{order: orderCode}}>{orderCode}</Link>
      </span>
    );
  }

  render() {
    const totals = _.get(this.props, ['storeCreditTotals', 'totals'], {});

    return (
      <div className="fc-store-credits">
        <Summary
          totals={totals}
          params={this.props.params}
          transactionsSelected={true}
        />
        <div className="fc-store-credits__list">
        <BulkMessages
          storePath="customers.storeCreditTransactions.bulk"
          module="customers.storeCreditTransactions"
          entity="store credit transaction"
          renderDetail={this.renderDetail}
          className={styles['bulk-message']}
        />
        <BulkActions
          module="customers.storeCreditTransactions"
          entity="store credit transaction"
          actions={this.bulkActions}
        >
          <SelectableSearchList
            exportEntity="storeCreditTransactions"
            exportTitle="Store Credit Transactions"
            bulkExport
            bulkExportAction={this.props.bulkExportAction}
            entity="customers.storeCreditTransactions"
            title="Transactions"
            emptyMessage="No transactions found."
            list={this.props.list}
            renderRow={this.renderRow}
            tableColumns={this.props.tableColumns}
            searchActions={this.props.actions}
            searchOptions={{singleSearch: true}}
          />
        </BulkActions>
        </div>
      </div>
    );
  }
}

const mapStateToProps = (state, props) => {
  return {
    list: _.get(state.customers, 'storeCreditTransactions.list', {}),
    storeCreditTotals: _.get(state.customers, 'storeCreditTotals[props.params.customerId]', {}),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    actions: bindActionCreators(StoreCreditTransactionsActions, dispatch),
    totalsActions: bindActionCreators(StoreCreditTotalsActions, dispatch),
    bulkExportAction: bindActionCreators(bulkExport, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(StoreCreditTransactions);
