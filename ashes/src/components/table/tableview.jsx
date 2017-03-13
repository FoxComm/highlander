// libs
import React, { Component, PropTypes } from 'react';

// components
import Table from './table';
import DataTable from './data-table';
import ActionsDropdown from '../bulk-actions/actions-dropdown';
import TablePaginator from './paginator';
import TablePageSize from './pagesize';
import ColumnSelector from './column-selector';

function getLine(position, items) {
  if (!items.length) {
    return;
  }

  return (
    <div className={`fc-table__${position}`}>
      {items.map((item, index) => React.cloneElement(item, { key: `${position}-${index}` }))}
    </div>
  );
}

const TableView = props => {
  let setState = null;
  if (props.setState) {
    setState = params => {
      props.setState({ ...props.data, ...params });
    };
  }

  let topItems = [];
  let bottomItems = [];

  if (props.selectableColumns.length) {
    const toggler = (
      <ColumnSelector setColumns={props.setColumnSelected}
                      columns={props.selectableColumns}
                      identifier={props.tableIdentifier} />
    );

    topItems.push(toggler);
  }

  // hold actions menu
  const showBulkActions = Boolean(props.bulkActions.length);
  if (showBulkActions) {
    const { bulkActions, toggledIds, allChecked, data:{ total } } = props;

    //disabled if no data or nothing selected
    const totalSelected = allChecked ? total - toggledIds.length : toggledIds.length;
    const disabled = total === 0 || totalSelected === 0;

    topItems.push(
      <ActionsDropdown actions={bulkActions}
                       disabled={disabled}
                       allChecked={allChecked}
                       toggledIds={toggledIds}
                       total={total} />
    );
  }

  const showPagination = props.paginator && props.setState;
  if (showPagination) {
    const { from, total, size } = props.data;
    const flexSeparator = <div className="fc-table__flex-separator" />;
    const tablePaginator = <TablePaginator total={total} from={from} size={size} setState={setState} />;

    topItems.push(flexSeparator, tablePaginator);

    bottomItems.push(<TablePageSize setState={setState} value={size} />, flexSeparator, tablePaginator);
  }

  const { headerControls = [], footerControls = [] } = props;

  topItems.push(...headerControls);
  bottomItems.push(...footerControls);

  const TableComponent = props.dataTable ? DataTable : Table;

  return (
    <div className="fc-tableview">
      {getLine('header', topItems)}
      <div className="fc-table__table">
        <TableComponent {...props} setState={setState} />
      </div>
      {getLine('footer', bottomItems)}
    </div>
  );
};

TableView.propTypes = {
  columns: PropTypes.array.isRequired,
  dataTable: PropTypes.bool,
  data: PropTypes.shape({
    rows: PropTypes.array,
    total: PropTypes.number,
    from: PropTypes.number,
    size: PropTypes.number,
  }),
  setState: PropTypes.func,
  renderRow: PropTypes.func,
  processRows: PropTypes.func,
  selectableColumns: PropTypes.array,
  setColumnSelected: PropTypes.func,
  tableIdentifier: PropTypes.string,
  detectNewRows: PropTypes.bool,
  paginator: PropTypes.bool,
  bulkActions: PropTypes.arrayOf(PropTypes.array),
  allChecked: PropTypes.bool,
  toggledIds: PropTypes.array,
  isLoading: PropTypes.bool,
  failed: PropTypes.bool,
  emptyMessage: PropTypes.string,
  errorMessage: PropTypes.string,
  className: PropTypes.string,
  renderHeadIfEmpty: PropTypes.bool,
  headerControls: PropTypes.array,
  footerControls: PropTypes.array,
};

TableView.defaultProps = {
  paginator: true,
  bulkActions: [],
  selectableColumns: [],
  dataTable: false,
  data: {
    rows: [],
    total: 0,
  },
};

export default TableView;
