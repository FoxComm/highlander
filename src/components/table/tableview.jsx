import React, { PropTypes } from 'react';
import Table from './table';
import TablePaginator from './paginator';
import TablePageSize from './pagesize';

const TableView = props => {
  let setState = null;
  if (props.setState) {
    setState = params => {
      props.setState({...props.data, ...params});
    };
  }

  const tableNeedsPagination = props.paginator && props.setState && props.data.total > 0;
  const tablePaginator = tableNeedsPagination && (
      <TablePaginator
        total={props.data.total}
        from={props.data.from}
        size={props.data.size}
        setState={setState}
        />
    );
  const tablePageSize = tableNeedsPagination && (
      <TablePageSize setState={setState}/>
    );
  return (
    <div className="fc-tableview">
      {tableNeedsPagination && (
        <div className="fc-table-header fc-grid fc-grid-no-gutter">
          <div className="fc-col-md-2-12 fc-push-md-10-12 fc-align-right">
            {tablePaginator}
          </div>
        </div>
      )}
      <Table {...props} setState={setState}/>
      {tableNeedsPagination && (
        <div className="fc-table-footer fc-grid fc-grid-no-gutter">
          <div className="fc-col-md-2-12 fc-align-left">
            {tablePageSize}
          </div>
          <div className="fc-col-md-2-12 fc-push-md-8-12 fc-align-right">
            {tablePaginator}
          </div>
        </div>
      )}
    </div>
  );
};

TableView.propTypes = {
  columns: PropTypes.array.isRequired,
  data: PropTypes.shape({
    rows: PropTypes.array,
    total: PropTypes.number,
    from: PropTypes.number,
    size: PropTypes.number
  }),
  setState: PropTypes.func,
  renderRow: PropTypes.func,
  processRows: PropTypes.func,
  detectNewRows: PropTypes.bool,
  paginator: PropTypes.bool
};

TableView.defaultProps = {
  paginator: true,
  data: {
    rows: [],
    total: 0,
  }
};

export default TableView;
