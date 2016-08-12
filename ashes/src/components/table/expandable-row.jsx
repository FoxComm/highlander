// libs
import React, { PropTypes } from 'react';
import _ from 'lodash';
import classNames from 'classnames';

// components
import TableCell from '../table/cell';
import TableRow from '../table/row';

function cells(columns, row, params, setCellContents) {
  return _.map(columns, col => {
    const cellKey = `row-${col.field}`;
    const cellContents = setCellContents(row, col.field);

    return (
      <TableCell onClick={params.toggleDrawerState} key={cellKey} column={col}>
        {cellContents}
      </TableCell>
    );
  });
}

const ExpandableRow = props => {
  const { columns, row, params, setCellContents, ...rest } = props;
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
  params: PropTypes.shape({
    toggleDrawerState: PropTypes.func.isRequired,
    isOpen: PropTypes.bool,
  }),
};

export default ExpandableRow;
