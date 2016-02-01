// libs
import React, { PropTypes } from 'react';
import _ from 'lodash';

// components
import { Checkbox } from '../checkbox/checkbox';
import TableCell from '../table/cell';
import TableRow from '../table/row';

const MultiSelectRow = props => {
  const { columns, onClick, row, setCellContents, checked, setChecked, ...rest } = props;

  const cells = _.reduce(columns, (visibleCells, col) => {
    const cellKey = `row-${col.field}`;
    let cellContents = null;
    let cellClickAction = onClick;

    const onChange = ({target: { checked }}) => {
      setChecked(checked);
    };

    switch (col.field) {
      case 'toggleColumns':
        cellContents = '';
        break;
      case 'selectColumn':
        cellClickAction = _.noop;
        cellContents = <Checkbox checked={checked} onChange={onChange} />;
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
  columns: PropTypes.array.isRequired,
  onClick: PropTypes.func,
  row: PropTypes.object.isRequired,
  setCellContents: PropTypes.func.isRequired,
  checked: PropTypes.bool.isRequired,
  setChecked: PropTypes.func.isRequired,
};

MultiSelectRow.defaultProps = {
  onClick: _.noop
};

export default MultiSelectRow;
