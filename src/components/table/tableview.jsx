'use strict';

import React, { PropTypes } from 'react';
import Table from './table';
import TablePaginator from './paginator';
import TablePageSize from './pagesize';

const TableView = (props) => {
  const tableNeedsPagination = props.paginator && props.data.total > 0;
  const tablePaginator = tableNeedsPagination && (
      <TablePaginator
        total={props.data.total}
        from={props.data.from}
        size={props.data.size}
        setFrom={props.setFrom}
        />
    );
  const tablePageSize = tableNeedsPagination && (
      <TablePageSize setSize={props.setSize}/>
    );
  return (
    <div className="fc-tableview">
      {tableNeedsPagination && (
        <div className="fc-table-header">
          {tablePaginator}
        </div>
      )}
      <Table columns={props.columns} data={props.data} renderRow={props.renderRow}/>
      {tableNeedsPagination && (
        <div className="fc-table-footer">
          {tablePageSize}
          {tablePaginator}
        </div>
      )}
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
