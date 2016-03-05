/** Libs */
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

/** Components */
import { SearchList } from '../list-page';
import Row from '../table/row';
import Cell from '../table/cell';

/** Redux */
import { actions } from '../../modules/inventory/transactions';

const tableColumns = [
  { field: 'placedAt', text: 'Date/time', type: 'datetime' },
  { field: 'event', text: 'Event' },
  { field: 'warehouse.name', text: 'Warehouse' },
  { field: 'type', text: 'Type' },
  { field: 'state', text: 'State' },
  { field: 'previous', text: 'Previous' },
  { field: 'new', text: 'New' },
  { field: 'change', text: 'Change' },
  { field: 'afs', text: 'New AFS' },
];

/** InventoryItemTransactions Component */
const InventoryItemTransactions = props => {

  const renderRow = (row, index, columns, params) => {
    const key = `inventory-transaction-${row.id}`;

    return <Row key={key}>{this.props.data.map(field => <Cell>{_.get(row, field)}</Cell>)}</Row>;
  };

  return (
    <SearchList
      emptyMessage="No transaction units found."
      list={props.list}
      renderRow={renderRow}
      tableColumns={tableColumns}
      searchOptions={{singleSearch: true}}
      searchActions={props.actions}
      setState={props.actions.updateStateAndFetch}
      className="fc-inventory-item-details__transactions-table"/>
  );
};

function mapState(state) {
  return {
    list: state.inventory.transactions
  };
}

function mapDispatch(dispatch) {
  return { actions: bindActionCreators(actions, dispatch) };
}

export default connect(mapState, mapDispatch)(InventoryItemTransactions);
