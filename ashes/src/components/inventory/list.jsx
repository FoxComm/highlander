// libs
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

// component
import { SelectableSearchList } from '../list-page';
import InventoryListRow from './inventory-list-row';

// redux
import { actions } from 'modules/inventory/list';

const getState = state => ({ list: state.inventory.list });

const mapDispatchToProps = dispatch => {
  return { actions: bindActionCreators(actions, dispatch) };
};

const tableColumns = [
  { field: 'sku', text: 'SKU' },
  { field: 'type', text: 'SKU Type' },
  { field: 'stockLocation.name', text: 'Warehouse' },
  { field: 'onHand', text: 'On Hand' },
  { field: 'onHold', text: 'Hold' },
  { field: 'reserved', text: 'Reserved' },
  { field: 'afs', text: 'AFS' },
  { field: 'afsCost', text: 'AFS Cost', type: 'currency' },
];

const InventoryList = props => {

  const renderRow = (row, index, columns, params) => {
    const key = `inventory-sku-${row.id}`;
    return <InventoryListRow sku={row} columns={columns} params={params} key={key} />;
  };

  return (
    <SelectableSearchList
      entity="inventory.list"
      emptyMessage="No inventory units found."
      list={props.list}
      renderRow={renderRow}
      tableColumns={tableColumns}
      searchActions={props.actions}
    />
  );
};

InventoryList.propTypes = {
  list: PropTypes.object.isRequired,
  actions: PropTypes.object.isRequired
};

export default connect(getState, mapDispatchToProps)(InventoryList);
