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
        setState={props.setState.bind(this, props.data)}
        />
    );
  const tablePageSize = tableNeedsPagination && (
      <TablePageSize setState={props.setState.bind(this, props.data)}/>
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
  setState: PropTypes.func.isRequired,
  renderRow: PropTypes.func,
  paginator: PropTypes.bool
};

TableView.defaultProps = {
  paginator: true
};

export default TableView;
