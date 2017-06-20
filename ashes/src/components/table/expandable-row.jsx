// libs
import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import classNames from 'classnames';

// components
import TableCell from '../table/cell';
import TableRow from '../table/row';

function cells(columns, row, params, setCellContents) {
  return _.map(columns, (col, index) => {
    const cellKey = `row-${col.field}`;
    const cellContents = setCellContents(row, col.field);
    const className = classNames({'icon-chevron-down': index === 0});

    return (
      <TableCell className={className} onClick={params.toggleDrawerState} key={cellKey} column={col}>
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
