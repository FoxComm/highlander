
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';

import { actions } from '../../modules/inventory/list';

const getState = state => ({ list: state.inventory.list });

const mapDispatchToProps = dispatch => {
  return { actions: bindActionCreators(actions, dispatch) };
};

const InventoryList = props => {
  return <div>Inventory</div>;
};

export default connect(getState, mapDispatchToProps)(InventoryList);
