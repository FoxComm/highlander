// libs
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

// component
import { SelectableSearchList } from '../list-page';
import SkuListRow from './sku-list-row';

// redux
import { actions } from 'modules/skus/list';

const getState = state => ({ list: state.skus.list });

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

const SkusList = props => {
  const renderRow = (row, index, columns, params) => {
    return <SkuListRow sku={row} columns={columns} params={params} key={row.id} />;
  };

  return (
    <SelectableSearchList
      entity="inventory.list"
      emptyMessage="No SKUs found."
      list={props.list}
      renderRow={renderRow}
      tableColumns={tableColumns}
      searchActions={props.actions}
    />
  );
};

SkusList.propTypes = {
  list: PropTypes.object.isRequired,
  actions: PropTypes.object.isRequired
};

export default connect(getState, mapDispatchToProps)(SkusList);
