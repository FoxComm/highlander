/** Libs */
import { get, isString, capitalize } from 'lodash';
import React, { PropTypes, Component } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import * as dsl from '../../elastic/dsl';

/** Components */
import { SearchList } from '../list-page';
import InventoryItemTransactionsRow from './item-transactions-row';

/** Redux */
import { actions } from '../../modules/inventory/transactions';

const tableColumns = [
  { field: 'createdAt', text: 'Date/time', type: 'datetime' },
  { field: 'event', text: 'Event' },
  { field: 'warehouse', text: 'Warehouse' },
  { field: 'skuType', text: 'Type', type: 'state', model: 'sku' },
  { field: 'state', text: 'State', type: 'state', model: 'skuState' },
  { field: 'previousQuantity', text: 'Previous' },
  { field: 'newQuantity', text: 'New' },
  { field: 'change', text: 'Change', type: 'change' },
  { field: 'newAfs', text: 'New AFS' },
];

const renderRow = (row) => {
  const keyRow = `inventory-transaction-${row.id}`;

  return <InventoryItemTransactionsRow transaction={row} columns={tableColumns} key={keyRow}/>;
};

/** InventoryItemTransactions Component */
class InventoryItemTransactions extends Component {

  componentDidMount() {
    this.props.actions.setExtraFilters([
      dsl.termFilter('skuCode', this.props.params.code)
    ]);

    this.props.actions.fetch();
  }

  render() {
    return (
      <SearchList
        emptyMessage="No transaction units found."
        list={this.props.list}
        renderRow={renderRow}
        tableColumns={tableColumns}
        searchOptions={{singleSearch: true}}
        searchActions={this.props.actions}
        setState={this.props.actions.updateStateAndFetch}
        className="fc-inventory-item-details__transactions-table"/>
    );
  }
}


function mapState(state) {
  return {
    list: state.inventory.transactions
  };
}

function mapDispatch(dispatch) {
  return { actions: bindActionCreators(actions, dispatch) };
}

export default connect(mapState, mapDispatch)(InventoryItemTransactions);
