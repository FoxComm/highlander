
// libs
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

// component
import { SearchableList } from '../list-page';

// redux
import { actions } from '../../modules/inventory/list';

const getState = state => ({ list: state.inventory.list });

const mapDispatchToProps = dispatch => {
  return { actions: bindActionCreators(actions, dispatch) };
};

const InventoryList = props => {

  const renderRow = () => { return <div>row</div>; };

  const tableColumns = [
    {field: 'product', text: 'Product'},
    {field: 'productActive', text: 'Product State'},
    {field: 'sku', text: 'SKU'},
    {field: 'skuType', text: 'SKU Type'},
    {field: 'warehouse', text: 'Warehouse', type: 'state'},
    {field: 'onHand', text: 'On Hand'},
    {field: 'onHold', text: 'Hold'},
    {field: 'reserved', text: 'Reserved'},
    {field: 'safetyStock', text: 'Safety Stock'},
    {field: 'afs', text: 'AFS'},
  ];

  return (
    <SearchableList
      emptyResultMessage="No SKUs found."
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
