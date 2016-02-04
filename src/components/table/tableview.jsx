// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';

// components
import Table from './table';
import ActionsDropdown from '../bulk-actions/actions-dropdown';
import TablePaginator from './paginator';
import TablePageSize from './pagesize';


function getLine(position, items) {
  if (!items.length) {
    return;
  }

  return (
    <div className={`fc-table-${position}`}>
      {items}
    </div>
  );
}

function getActionsHandler({bulkActions, allChecked, toggledIds}) {
  return (value) => {
    const handler = _.find(bulkActions, ([label, handler]) => label === value)[1];
    handler(allChecked, toggledIds);
  };
}

const TableView = props => {
  let setState = null;
  if (props.setState) {
    setState = params => {
      props.setState({...props.data, ...params});
    };
  }

  let topItems = [];
  let bottomItems = [];

  // hold actions menu
  const showBulkActions = Boolean(props.bulkActions.length);
  if (showBulkActions) {
    topItems.push(
      <ActionsDropdown actions={props.bulkActions}
                       allChecked={props.allChecked}
                       toggledIds={props.toggledIds}
                       total={props.data.total} />
    );
  }

  const showPagination = props.paginator && props.setState;
  if (showPagination) {
    const {from, total, size} = props.data;
    const flexSeparator = <div className="fc-table-flex-separator" />;
    const tablePaginator = <TablePaginator total={total} from={from} size={size} setState={setState} />;

    topItems.push(flexSeparator, tablePaginator);

    bottomItems.push(<TablePageSize setState={setState} />, flexSeparator, tablePaginator);
  }

  return (
    <div className="fc-tableview">
      {getLine('header', topItems)}
      <Table {...props} setState={setState} />
      {getLine('bottom', bottomItems)}
    </div>
  );
};

TableView.propTypes = {
  columns: PropTypes.array.isRequired,
  data: PropTypes.shape({
    rows: PropTypes.array,
    total: PropTypes.number,
    from: PropTypes.number,
    size: PropTypes.number,
  }),
  setState: PropTypes.func,
  renderRow: PropTypes.func,
  processRows: PropTypes.func,
  detectNewRows: PropTypes.bool,
  paginator: PropTypes.bool,
  bulkActions: PropTypes.arrayOf(PropTypes.array),
  allChecked: PropTypes.bool,
  toggledIds: PropTypes.array,
  showEmptyMessage: PropTypes.bool,
  emptyMessage: PropTypes.string,
  className: PropTypes.string,
};

TableView.defaultProps = {
  paginator: true,
  bulkActions: [],
  data: {
    rows: [],
    total: 0,
  },
};

export default TableView;
