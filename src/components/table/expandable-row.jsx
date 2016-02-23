// libs
import React, { PropTypes } from 'react';
import _ from 'lodash';
import classNames from 'classnames';

// components
import TableCell from '../table/cell';
import TableRow from '../table/row';
import Drawer from './drawer';

function cells(columns, row, params, setCellContents) {
  const cells = _.reduce(columns, (visibleCells, col) => {
    const cellKey = `row-${col.field}`;
    const cellContents = setCellContents(row, col.field);
    visibleCells.push(
      <TableCell onClick={params.toggleDrawerState} key={cellKey} column={col}>
        {cellContents}
      </TableCell>
    );
    return visibleCells;
  }, []);

  return cells;
}

const ExpandableRow = props => {
  const { columns, row, params, setDrawerContent, setCellContents, ...rest } = props;
  const parentRowClass = classNames('fc-expandable-table__parent-row', {
    '_drawer-open': params.isOpen
  });

  const rowCells = cells(columns, row, params, setCellContents);

  return (
    <TableRow className={parentRowClass} {...rest}>
      {rowCells}
    </TableRow>
  );
};

ExpandableRow.propTypes = {
  columns: PropTypes.array.isRequired,
  row: PropTypes.object.isRequired,
  setCellContents: PropTypes.func.isRequired,
  params: PropTypes.object,
  toggleDrawerState: PropTypes.func.isRequired,
  isOpen: PropTypes.bool,
};

ExpandableRow.defaultProps = {
  isOpen: false,
};

export default ExpandableRow;
