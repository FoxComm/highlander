// libs
import React, { PropTypes } from 'react';
import _ from 'lodash';
import classNames from 'classnames';
import { transitionTo } from 'browserHistory';

// components
import { Checkbox } from '../checkbox/checkbox';
import TableCell from '../table/cell';
import TableRow from '../table/row';

const stopPropogation = (event: MouseEvent) => {
  event.preventDefault();
  event.stopPropagation();
}

const MultiSelectRow = (props, context) => {
  const {
    columns,
    row,
    setCellContents,
    collapseField,
    collapsible,
    collapsed,
    level,
    params: { checked, setChecked, toggleCollapse },
    linkTo,
    linkParams,
    ...rest,
  } = props;

  let { onClick, href } = rest;

  // linkTo is shortcut for creating onClick and href properties which leads to defined behaviour:
  // single click leads to transition
  // over click leads to page load
  if (linkTo) {
    onClick = (event) => {
      if (event.button == 0 && !event.ctrlKey) {
        stopPropogation(event);
        transitionTo(linkTo, linkParams);
      }
    };

    href = context.router.createHref({ name: linkTo, params: linkParams });
  }

  const cells = _.reduce(columns, (visibleCells, col) => {
    const cellKey = `row-${col.field}`;
    let cellContents = null;
    let cellClickAction = null;

    const onChange = ({ target: { checked } }) => {
      setChecked(checked);
    };
    const onCollapse = (event: MouseEvent) => {
      stopPropogation(event);

      toggleCollapse(row);
    };

    let cls = classNames(`fct-row__${col.field}`, {
      'row-head-left': col.field == 'selectColumn',
    });

    switch (col.field) {
      case collapseField:
        const iconClassName = classNames(({
          'icon-category': !collapsible,
          'icon-category-expand': collapsible && collapsed,
          'icon-category-collapse': collapsible && !collapsed,
        }));
        cellContents = (
          <span className="fc-collapse" style={{ paddingLeft: `${level * 20}px`}}>
            <i className={iconClassName} onClick={onCollapse} />
            {setCellContents(row, col.field)}
          </span>
        );
        cls = classNames(cls, 'fct-row__collapse');
        break;
      case 'selectColumn':
        cellClickAction = stopPropogation;
        cellContents = <Checkbox id={`multi-select-${row.id}`} inline={true} checked={checked} onChange={onChange} />;
        break;
      default:
        const setCellFn = setCellContents || _.get;
        cellContents = setCellFn(row, col.field);
        break;
    }

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
  collapseField: PropTypes.string,
  params: PropTypes.shape({
    checked: PropTypes.bool.isRequired,
    setChecked: PropTypes.func.isRequired,
    toggleCollapse: PropTypes.func,
  }),
};

MultiSelectRow.contextTypes = {
  router: PropTypes.object.isRequired,
};

MultiSelectRow.defaultProps = {
  onClick: _.noop,
  params: {
    toggleCollapse: _.noop,
  }
};

export default MultiSelectRow;
