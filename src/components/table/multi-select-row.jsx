import React, { PropTypes } from 'react';
import _ from 'lodash';

import { Checkbox } from '../checkbox/checkbox';
import TableCell from '../table/cell';
import TableRow from '../table/row';

const MultiSelectRow = props => {
  const { cellKeyPrefix, columns, onClick, row, setCellContents, ...rest } = props;

  const cells = _.reduce(columns, (visibleCells, col) => {
    const cellKey = `${cellKeyPrefix}-${col.field}`;
    let cellContents = null;
    let cellClickAction = onClick;

    switch(col.field) {
      case 'toggleColumns':
        cellContents = '';
        break;
      case 'selectColumn':
        cellClickAction = _.noop;
        cellContents = <Checkbox />;
        break;
      default:
        cellContents = setCellContents(row, col.field);
        break;
    }

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

MultiSelectRow.propTypes = {
  cellKeyPrefix: PropTypes.string.isRequired,
  columns: PropTypes.array.isRequired,
  onClick: PropTypes.func,
  row: PropTypes.object.isRequired,
  setCellContents: PropTypes.func.isRequired
};

MultiSelectRow.defaultProps = {
  onClick: _.noop
};

export default MultiSelectRow;
