/* @flow weak */

import React, { Component } from 'react';

// libs
import _ from 'lodash';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import * as dsl from 'elastic/dsl';
import { autobind } from 'core-decorators';
import { bulkExportBulkAction, renderExportModal } from 'modules/bulk-export/helpers';

// actions
import { actions } from 'modules/inventory/transactions/transactions';
import { bulkExport } from 'modules/bulk-export/bulk-export';
import { actions as bulkActions } from 'modules/inventory/transactions/bulk';

// components
import { SelectableSearchList } from 'components/list-page';
import InventoryItemTransactionsRow from './item-transactions-row';
import BulkActions from 'components/bulk-actions/bulk-actions';
import BulkMessages from 'components/bulk-actions/bulk-messages';

type Actions = {
  setExtraFilters: Function,
  fetch: Function,
  updateStateAndFetch: Function,
};

type Params = {
  skuCode: string,
};

type Props = {
  actions: Actions,
  params: Params,
  list: Object,
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
  { field: 'createdAt', text: 'Date/Time', type: 'datetime' },
  { field: 'stockLocationName', text: 'Warehouse' },
  { field: 'type', text: 'Type' },
  { field: 'status', text: 'State', type: 'state', model: 'skuState' },
  { field: 'quantityPrevious', text: 'Previous' },
  { field: 'quantityNew', text: 'New' },
  { field: 'quantityChange', text: 'Change', type: 'change' },
  { field: 'afsNew', text: 'New AFS' },
];

class InventoryItemTransactions extends Component {
  props: Props;

  componentDidMount() {
    this.props.actions.setExtraFilters([
      dsl.termFilter('sku', this.props.params.skuCode)
    ]);

    this.props.actions.fetch();
  }

  @autobind
  renderRow(row: Object, index: number, columns: Columns, params: Object) {
    const key = `inventory-transaction-${row.id}`;

    return (
      <InventoryItemTransactionsRow
        transaction={row}
        columns={columns}
        key={key}
        params={params}
      />
    );
  }

  @autobind
  bulkExport(allChecked: boolean, toggledIds: Array<number>) {
    const { exportByIds } = this.props.bulkActions;
    const modalTitle = 'Inventory Transactions';
    const entity = 'inventoryTransactions';

    return renderExportModal(tableColumns, entity, modalTitle, exportByIds, toggledIds);
  }

  get bulkActions(): Array<any> {
    return [
      bulkExportBulkAction(this.bulkExport, 'Inventory Transactions'),
    ];
  }

  @autobind
  renderBulkDetails(message: string, id: number) {
    return (
      <span key={id}>
        Inventory Transaction #{id}
      </span>
    );
  }


  render() {
    return (
      <div>
        <BulkMessages
          storePath="inventory.transactions.bulk"
          module="inventory.transactions"
          entity="inventory transaction"
          renderDetail={this.renderBulkDetails}
        />
        <BulkActions
          module="inventory.transactions"
          entity="inventory transaction"
          actions={this.bulkActions}
        >
          <SelectableSearchList
            exportEntity="inventoryTransactions"
            exportTitle="Inventory Transactions"
            bulkExport
            bulkExportAction={this.props.bulkExportAction}
            entity="inventory.transactions.list"
            emptyMessage="No transaction units found."
            list={this.props.list}
            renderRow={this.renderRow}
            tableColumns={tableColumns}
            searchOptions={{singleSearch: true}}
            searchActions={this.props.actions}
            setState={this.props.actions.updateStateAndFetch}
          />
        </BulkActions>
      </div>
    );
  }
}


const mapStateToProps = (state) => {
  return {
    list: _.get(state.inventory, 'transactions.list', {}),
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkExportAction: bindActionCreators(bulkExport, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(InventoryItemTransactions);
