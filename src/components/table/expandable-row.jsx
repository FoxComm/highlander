// libs
import React, { PropTypes } from 'react';
import _ from 'lodash';

// components
import TableCell from '../table/cell';
import TableRow from '../table/row';

const ExpandableRow = props => {
  const { columns, onClick, row, setCellContents, ...rest } = props;

  const cells = _.reduce(columns, (visibleCells, col) => {
    const cellKey = `row-${col.field}`;

    const cellClickAction = _.noop;
    const cellContents = setCellContents(row, col.field);

    visibleCells.push(
      <TableCell onClick={cellClickAction} key={cellKey} column={col}>
        {cellContents}
      </TableCell>
    );

    return visibleCells;
  }, []);

  return (
    <TableRow {...rest}>
      {cells}
    </TableRow>
  );
};

ExpandableRow.propTypes = {
  columns: PropTypes.array.isRequired,
  row: PropTypes.object.isRequired,
  setCellContents: PropTypes.func.isRequired,
};

ExpandableRow.defaultProps = {
  onClick: _.noop
};

export default ExpandableRow;
