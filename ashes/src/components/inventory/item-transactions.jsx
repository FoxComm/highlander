/* @flow weak */

import React, { Component } from 'react';

// libs
import _ from 'lodash';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import * as dsl from 'elastic/dsl';
import { autobind } from 'core-decorators';

// actions
import { actions } from 'modules/inventory/transactions';
import { bulkExport } from 'modules/bulk-export/bulk-export';

// components
import { SelectableSearchList } from 'components/list-page';
import InventoryItemTransactionsRow from './item-transactions-row';

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
  renderRow(row: Object, index: number, columns: Array<Object>, params: Object) {
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

  render() {
    return (
      <SelectableSearchList
        exportEntity="inventoryTransactions"
        exportTitle="Inventory Transactions"
        bulkExport
        bulkExportAction={this.props.bulkExportAction}
        entity="inventory.transactions"
        emptyMessage="No transaction units found."
        list={this.props.list}
        renderRow={this.renderRow}
        tableColumns={tableColumns}
        searchOptions={{singleSearch: true}}
        searchActions={this.props.actions}
        setState={this.props.actions.updateStateAndFetch}
      />
    );
  }
}


const mapStateToProps = (state) => {
  return {
    list: _.get(state.inventory, 'transactions', {}),
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkExportAction: bindActionCreators(bulkExport, dispatch),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(InventoryItemTransactions);
