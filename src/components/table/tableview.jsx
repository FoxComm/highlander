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
    <div className={`fc-table__${position}`}>
      {items.map((item, index) => React.cloneElement(item, {key: `${position}-${index}`}))}
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
    const {bulkActions, toggledIds, allChecked, data:{total}} = props;

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
    const {from, total, size} = props.data;
    const flexSeparator = <div className="fc-table__flex-separator" />;
    const tablePaginator = <TablePaginator total={total} from={from} size={size} setState={setState} />;

    topItems.push(flexSeparator, tablePaginator);

    bottomItems.push(<TablePageSize setState={setState} value={size} />, flexSeparator, tablePaginator);
  }

  return (
    <div className="fc-tableview">
      {getLine('header', topItems)}
      <div className="fc-table__table">
        <Table {...props} setState={setState} />
      </div>
      {getLine('footer', bottomItems)}
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
