
/* @flow weak */

/** Libs */
import { get, isString, capitalize } from 'lodash';
import React, { PropTypes, Component } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import * as dsl from 'elastic/dsl';

/** Components */
import SearchList from 'components/list-page/search-list';
import InventoryItemTransactionsRow from './sku-transactions-row';

/** Redux */
import { actions } from 'modules/skus/transactions';

type Actions = {
  setExtraFilters: Function,
  fetch: Function,
  updateStateAndFetch: Function,
};


type Props = {
  skuId: number,
  // connected
  actions: Actions,
  list: Object,
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

const renderRow = (row, index, columns) => {
  return <InventoryItemTransactionsRow transaction={row} columns={columns} key={row.id}/>;
};

/** InventoryItemTransactions Component */
class InventoryItemTransactions extends Component {
  props: Props;

  componentDidMount() {
    this.props.actions.setExtraFilters([
      // @TODO: jeff promised to add `skuId` to inventory_transactions_search_view
      dsl.termFilter('skuId', this.props.skuId)
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
      />
    );
  }
}


function mapState(state) {
  return {
    list: state.skus.transactions
  };
}

function mapDispatch(dispatch) {
  return { actions: bindActionCreators(actions, dispatch) };
}

export default connect(mapState, mapDispatch)(InventoryItemTransactions);
