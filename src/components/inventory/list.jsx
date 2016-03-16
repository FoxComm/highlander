
// libs
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

// component
import { SelectableSearchList } from '../list-page';
import InventoryListRow from './inventory-list-row';

// redux
import { actions } from '../../modules/inventory/list';

const getState = state => ({ list: state.inventory.list });

const mapDispatchToProps = dispatch => {
  return { actions: bindActionCreators(actions, dispatch) };
};

const InventoryList = props => {

  const renderRow = (row, index, columns, params) => {
    const key = `inventory-sku-${row.id}`;
    return <InventoryListRow sku={row} columns={columns} params={params} />;
  };

  const tableColumns = [
    {field: 'product', text: 'Product'},
    {field: 'productActive', text: 'Product State'},
    {field: 'code', text: 'SKU'},
    {field: 'skuActive', text: 'SKU State'},
    {field: 'skuType', text: 'SKU Type', type: 'state', model: 'sku'},
    {field: 'warehouse', text: 'Warehouse'},
    {field: 'onHand', text: 'On Hand'},
    {field: 'onHold', text: 'Hold'},
    {field: 'reserved', text: 'Reserved'},
    {field: 'safetyStock', text: 'Safety Stock'},
    {field: 'afs', text: 'AFS'},
  ];

  return (
    <SelectableSearchList
      emptyMessage="No inventory units found."
      list={props.list}
      renderRow={renderRow}
      tableColumns={tableColumns}
      searchActions={props.actions} />
  );
};

InventoryList.propTypes = {
  list: PropTypes.object.isRequired,
  actions: PropTypes.object.isRequired
};

export default connect(getState, mapDispatchToProps)(InventoryList);
