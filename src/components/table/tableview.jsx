// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';

// components
import Table from './table';
import TablePaginator from './paginator';
import TablePageSize from './pagesize';
import {Dropdown, DropdownItem} from '../dropdown';


function getLine(position, items) {
  if (!items.length) {
    return;
  }

  return (
    <div className={`fc-table-${position} fc-grid fc-grid-no-gutter`}>
      {items}
    </div>
  );
}

function getClassName({col, direction, offset, align, table='md'}) {
  return classNames(
    `fc-col-${table}-${col}-12`,
    {[`fc-pull-${table}-${offset}-12`]: direction === 'pull'},
    {[`fc-push-${table}-${offset}-12`]: direction === 'push'},
    {'fc-align-left': align === 'left'},
    {'fc-align-right': align === 'right'},
  );
}

function getActionsHandler({bulkActions, allChecked, checkedIds}) {
  return (value) => {
    const handler = _.find(bulkActions, ([label, handler]) => label === value)[1];
    handler(allChecked, checkedIds);
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
    const totalSelected = props.allChecked ? props.data.total - props.checkedIds.length : props.checkedIds.length;

    topItems.push(
      <div className={getClassName({col: 3, align: 'left'})}>
        <Dropdown placeholder="Actions"
                  changeable={false}
                  onChange={getActionsHandler(props)}>
          {props.bulkActions.map(([title, handler]) => (
            <DropdownItem key={title} value={title}>{title}</DropdownItem>
          ))}
        </Dropdown>
        {totalSelected} Selected
      </div>
    );
  }

  const showPagination = props.paginator && props.setState;
  if (showPagination) {
    const {data} = props;

    const tablePaginator = (
      <TablePaginator total={data.total} from={data.from} size={data.size} setState={setState} />
    );
    const tablePageSize = (
      <TablePageSize setState={setState} />
    );

    topItems.push(
      <div className={getClassName({col: 2, direction: 'push', offset: showBulkActions ? 7 : 10, align: 'right'})}>
        {tablePaginator}
      </div>
    );

    bottomItems.push(
      <div className={getClassName({col: 2, align: 'left'})}>
        {tablePageSize}
      </div>
    );
    bottomItems.push(
      <div className={getClassName({col: 2, direction: 'push', offset: 8, align: 'right'})}>
        {tablePaginator}
      </div>
    );
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
  checkedIds: PropTypes.array,
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
