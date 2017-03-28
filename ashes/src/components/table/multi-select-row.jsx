// libs
import React, { PropTypes } from 'react';
import _ from 'lodash';
import classNames from 'classnames';
import { transitionTo } from 'browserHistory';

// components
import { Checkbox } from '../checkbox/checkbox';
import TableCell from '../table/cell';
import TableRow from '../table/row';

const MultiSelectRow = (props, context) => {
  const {
    columns,
    row,
    setCellContents,
    processCell,
    params: { checked, setChecked },
    linkTo,
    linkParams,
    ...rest,
  } = props;

  let { onClick, href } = rest;

  // linkTo is shortcut for creating onClick and href properties which leads to defined behaviour:
  // single click leads to transition
  // over click leads to page load
  if (linkTo) {
    onClick = (event: MouseEvent) => {
      if (event.button == 0 && !event.ctrlKey) {
        event.preventDefault();
        event.stopPropagation();

        transitionTo(linkTo, linkParams);
      }
    };

    href = context.router.createHref({ name: linkTo, params: linkParams });
  }

  const onChange = ({ target: { checked } }) => setChecked(checked);

  const cells = _.reduce(columns, (visibleCells, col) => {
    const cellKey = `row-${col.field}`;
    let cellContents = null;
    let cellClickAction = null;

    const cls = classNames(`fct-row__${col.field}`, {
      'row-head-left': col.field == 'selectColumn',
    });

    switch (col.field) {
      case 'selectColumn':
        cellClickAction = (event: MouseEvent) => event.stopPropagation();
        cellContents = <Checkbox id={`multi-select-${row.id}`} inline={true} checked={checked} onChange={onChange} />;
        break;
      default:
        const setCellFn = setCellContents || _.get;
        cellContents = setCellFn(row, col.field);
        break;
    }

    visibleCells.push(
      <TableCell className={cls} onClick={cellClickAction} key={cellKey} column={col} row={row}>
        {processCell(cellContents, col)}
      </TableCell>
    );

    return visibleCells;
  }, []);

  return (
    <TableRow {...rest} href={href} onClick={onClick}>
      {cells}
    </TableRow>
  );
};

MultiSelectRow.propTypes = {
  columns: PropTypes.array.isRequired,
  onClick: PropTypes.func,
  href: PropTypes.string,
  linkTo: PropTypes.string,
  linkParams: PropTypes.object,
  row: PropTypes.object.isRequired,
  setCellContents: PropTypes.func.isRequired,
  processCell: PropTypes.func,
  params: PropTypes.shape({
    checked: PropTypes.bool.isRequired,
    setChecked: PropTypes.func.isRequired,
  }),
};

MultiSelectRow.contextTypes = {
  router: PropTypes.object.isRequired,
};

MultiSelectRow.defaultProps = {
  processCell: _.identity,
  onClick: _.noop,
};

export default MultiSelectRow;
