/** Libs */
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

/** Components */
import { SearchableList } from '../list-page';
import {} from './item-transactions-list-row.jsx';

/** Redux */
import { actions } from '../../modules/inventory/transactions';


/** InventoryItemTransactions Component */
const InventoryItemTransactions = props => {

  const renderRow = (row, index, columns, params) => {
    const key = `inventory-transaction-${row.id}`;
    return <InventoryListRow sku={row} columns={columns} params={params}/>;
  };

  return (
    <SearchableList
      emptyResultMessage="No transaction units found."
      list={props.list}
      renderRow={() => <div>item</div>}
      tableColumns={tableColumns}
      searchActions={props.actions}/>
  );
};

function mapState(state) {
  return {
    list: state.inventory.transactions.list
  };
}

function mapDispatch(dispatch) {
  return {actions: bindActionCreators(actions, dispatch)};
}

export default connect(mapState, mapDispatch)(InventoryItemTransactions);
