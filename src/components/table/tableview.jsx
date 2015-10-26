'use strict';

import React from 'react';
import Table from './table';

const TableView = (props) => {
  return (
    <div className="fc-tableview">
      <Table data={props.data} renderRow={props.renderRow}/>
    </div>
  );
};

TableView.propTypes = {
  renderRow: React.PropTypes.func,
  paginator: React.PropTypes.bool
};

TableView.defaultProps = {
  paginator: true
};

export default TableView;
