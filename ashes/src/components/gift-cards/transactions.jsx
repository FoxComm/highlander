/* @flow */

import React, { Component } from 'react';

// libs
import _ from 'lodash';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { bulkExportBulkAction, renderExportModal } from 'modules/bulk-export/helpers';

// actions
import { actions } from 'modules/gift-cards/transactions/transactions';
import { bulkExport } from 'modules/bulk-export/bulk-export';
import { actions as bulkActions } from 'modules/gift-cards/transactions/bulk';

// components
import { SelectableSearchList } from 'components/list-page';
import GiftCardTransactionRow from './gift-card-transaction-row';
import BulkActions from 'components/bulk-actions/bulk-actions';
import BulkMessages from 'components/bulk-actions/bulk-messages';
import { Link } from 'components/link';

type Props = {
  tableColumns: Array<Object>,
  actions: Object,
  list: Object,
  giftCard: Object,
  params: Object,
  bulkExportAction: (fields: Array<string>, entity: string, identifier: string, description: string) => Promise<*>,
  bulkActions: {
    exportByIds: (
      ids: Array<number>,
      description: string,
      fields: Array<Object>,
      entity: string,
      identifier: string
    ) => void,
  },
};
class GiftCardTransactions extends Component {
  props: Props;

  static defaultProps = {
    tableColumns: [
      { field: 'createdAt', text: 'Date/Time', type: 'date' },
      { field: 'orderPayment', text: 'Order', type: 'id', model: 'order', id: 'orderRef' },
      { field: 'debit', text: 'Amount', type: 'transaction' },
      { field: 'state', text: 'Payment State' },
      { field: 'availableBalance', text: 'Available Balance', type: 'currency' },
    ],
  };

  componentDidMount() {
    this.props.actions.setExtraFilters([{ term: { code: this.props.params.giftCard } }]);
    this.props.actions.fetch();
  }

  @autobind
  renderRow(row: Object, index: number, columns: Columns, params: Object) {
    const key = `gift-card-${index}`;

    return <GiftCardTransactionRow key={key} giftCard={row} columns={columns} params={params} />;
  }

  @autobind
  bulkExport(allChecked: boolean, toggledIds: Array<number>) {
    const { exportByIds } = this.props.bulkActions;
    const { tableColumns } = this.props;
    const modalTitle = 'Gift Card Transactions';
    const entity = 'giftCardTransactions';

    return renderExportModal(tableColumns, entity, modalTitle, exportByIds, toggledIds);
  }

  get bulkActions(): Array<any> {
    return [bulkExportBulkAction(this.bulkExport, 'Gift Card Transactions')];
  }

  renderBulkDetails(context: string, orderCode: string) {
    return (
      <span key={orderCode}>
        Transaction for order <Link to="order-details" params={{ order: orderCode }}>{orderCode}</Link>
      </span>
    );
  }

  render() {
    return (
      <div className="fc-gift-card-transactions">
        <BulkMessages
          storePath="giftCards.transactions.bulk"
          module="giftCards.transactions"
          entity="gift card transaction"
          renderDetail={this.renderBulkDetails}
        />
        <BulkActions module="giftCards.transactions" entity="gift card transaction" actions={this.bulkActions}>
          <SelectableSearchList
            exportEntity="giftCardTransactions"
            exportTitle="Gift Card Transactions"
            bulkExport
            bulkExportAction={this.props.bulkExportAction}
            entity="giftCards.transactions.list"
            emptyMessage="No transactions found."
            list={this.props.list}
            renderRow={this.renderRow}
            tableColumns={this.props.tableColumns}
            searchActions={this.props.actions}
            searchOptions={{ singleSearch: true }}
          />
        </BulkActions>
      </div>
    );
  }
}

const mapStateToProps = state => {
  return {
    list: _.get(state.giftCards, 'transactions.list', {}),
    giftCard: _.get(state.giftCards, 'details[props.params.giftCard]', {}),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkExportAction: bindActionCreators(bulkExport, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(GiftCardTransactions);
