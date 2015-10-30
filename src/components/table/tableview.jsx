'use strict';

import React, { PropTypes } from 'react';
import Table from './table';
import TablePaginator from './paginator';

const TableView = (props) => {
  return (
    <div className="fc-tableview">
      <TablePaginator
        total={props.data.total}
        from={props.data.from}
        size={props.data.size}
        setFrom={props.setFrom}
        />
      <Table columns={props.columns} data={props.data} renderRow={props.renderRow}/>
    </div>
  );
};

TableView.propTypes = {
  columns: PropTypes.array.isRequired,
  data: PropTypes.object.isRequired,
  renderRow: React.PropTypes.func,
  paginator: React.PropTypes.bool
};

TableView.defaultProps = {
  paginator: true
};

export default TableView;
