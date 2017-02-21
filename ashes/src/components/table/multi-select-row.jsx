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
  // params here is optional `context` for checkbox work which contains {checked, setChecked} props
  const { columns, row, setCellContents, params = {}, linkTo, linkParams, ...rest } = props;
  let { onClick, href } = rest;

  // linkTo is shortcut for creating onClick and href properties which leads to defined behaviour:
  // left click leads to transition
  // other clicks leads to page load
  if (linkTo) {
    onClick = (event) => {
      if (event.button == 0 && !event.ctrlKey) {
        event.preventDefault();
        transitionTo(linkTo, linkParams);
      }
    };

    href = context.router.createHref({name: linkTo, params: linkParams});
  }

  const cells = _.reduce(columns, (visibleCells, col) => {
    const cellKey = `row-${col.field}`;
    let cellContents = null;
    let cellClickAction = null;

    const onChange = ({target: { checked }}) => {
      params.setChecked(checked);
    };

    switch (col.field) {
      case 'toggleColumns':
        cellContents = '';
        break;
      case 'selectColumn':
        cellClickAction = event => event.stopPropagation();
        const checkbox = (
          <Checkbox id={`multi-select-${row.id}`} inline checked={params.checked} onChange={onChange} key="cb" />
        );
        cellContents = setCellContents(row, col.field, {
          checkbox,
        });
        if (!cellContents) {
          cellContents = checkbox;
        }
        break;
      default:
        cellContents = setCellContents(row, col.field);
        break;
    }

    const cls = classNames(`fct-row__${col.field}`, {
      'row-head-left': col.field == 'selectColumn',
      'row-head-right': col.field == 'toggleColumns'
    });

    visibleCells.push(
      <TableCell className={cls} onClick={cellClickAction} key={cellKey} column={col} row={row}>
        {cellContents}
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
  params: PropTypes.shape({
    checked: PropTypes.bool,
    setChecked: PropTypes.func,
  }),
};

MultiSelectRow.contextTypes = {
  router: PropTypes.object.isRequired,
};

MultiSelectRow.defaultProps = {
  onClick: _.noop
};

export default MultiSelectRow;
