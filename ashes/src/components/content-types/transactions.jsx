/* @flow */

import React, { Component } from 'react';

// libs
import _ from 'lodash';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { bulkExportBulkAction, renderExportModal } from 'modules/bulk-export/helpers';

// actions
import { actions } from 'modules/content-types/transactions/transactions';
import { bulkExport } from 'modules/bulk-export/bulk-export';
import { actions as bulkActions } from 'modules/content-types/transactions/bulk';

// components
import { SelectableSearchList } from 'components/list-page';
import ContentTypeTransactionRow from './content-type-transaction-row';
import BulkActions from 'components/bulk-actions/bulk-actions';
import BulkMessages from 'components/bulk-actions/bulk-messages';
import { Link } from 'components/link';

type Props = {
  tableColumns: Array<Object>,
  actions: Object,
  list: Object,
  contentType: Object,
  params: Object,
  bulkExportAction: (
    fields: Array<string>, entity: string, identifier: string, description: string
  ) => Promise<*>,
  bulkActions: {
    exportByIds: (
      ids: Array<number>, description: string, fields: Array<Object>, entity: string, identifier: string
    ) => void,
  },
};
class ContentTypeTransactions extends Component {
  props: Props;

  static defaultProps = {
    tableColumns: [
      {field: 'createdAt', text: 'Date/Time', type: 'date'},
      {field: 'orderPayment', text: 'Order', type: 'id', model: 'order', id: 'orderRef'},
      {field: 'debit', text: 'Amount', type: 'transaction'},
      {field: 'state', text: 'Payment State'},
      {field: 'availableBalance', text: 'Available Balance', type: 'currency'}
    ]
  };

  componentDidMount() {
    this.props.actions.setExtraFilters([
      {term: {code: this.props.params.contentType}}
    ]);
    this.props.actions.fetch();
  }

  @autobind
  renderRow(row: Object, index: number, columns: Columns, params: Object) {
    const key = `content-type-${index}`;

    return (
      <ContentTypeTransactionRow
        key={key}
        contentType={row}
        columns={columns}
        params={params}
      />
    );
  }

  @autobind
  bulkExport(allChecked: boolean, toggledIds: Array<number>) {
    const { exportByIds } = this.props.bulkActions;
    const { tableColumns } = this.props;
    const modalTitle = 'Content Type Transactions';
    const entity = 'contentTypeTransactions';

    return renderExportModal(tableColumns, entity, modalTitle, exportByIds, toggledIds);
  }

  get bulkActions(): Array<any> {
    return [
      bulkExportBulkAction(this.bulkExport, 'Content Type Transactions'),
    ];
  }

  renderBulkDetails(context: string, orderCode: string) {
    return (
      <span key={orderCode}>
        Transaction for order <Link to="order-details" params={{order: orderCode}}>{orderCode}</Link>
      </span>
    );
  }

  render() {
    return (
      <div className="fc-content-type-transactions">
        <BulkMessages
          storePath="contentTypes.transactions.bulk"
          module="contentTypes.transactions"
          entity="content type transaction"
          renderDetail={this.renderBulkDetails}
        />
        <BulkActions
          module="contentTypes.transactions"
          entity="content type transaction"
          actions={this.bulkActions}
        >
          <SelectableSearchList
            exportEntity="contentTypeTransactions"
            exportTitle="Content Type Transactions"
            bulkExport
            bulkExportAction={this.props.bulkExportAction}
            entity="contentTypes.transactions.list"
            emptyMessage="No transactions found."
            list={this.props.list}
            renderRow={this.renderRow}
            tableColumns={this.props.tableColumns}
            searchActions={this.props.actions}
            searchOptions={{singleSearch: true}}
          />
        </BulkActions>
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    list: _.get(state.contentTypes, 'transactions.list', {}),
    contentType: _.get(state.contentTypes, 'details[props.params.contentType]', {}),
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkExportAction: bindActionCreators(bulkExport, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(ContentTypeTransactions);
