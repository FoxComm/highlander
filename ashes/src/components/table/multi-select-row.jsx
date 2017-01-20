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
  const { columns, row, setCellContents, params: {checked, setChecked}, linkTo, linkParams, ...rest } = props;
  let { onClick, href } = rest;

  // linkTo is shortcut for creating onClick and href properties which leads to defined behaviour:
  // single click leads to transition
  // over click leads to page load
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
      setChecked(checked);
    };

    switch (col.field) {
      case 'toggleColumns':
        cellContents = '';
        break;
      case 'selectColumn':
        cellClickAction = event => event.stopPropagation();
        cellContents = <Checkbox id={`multi-select-${row.id}`} inline={true} checked={checked} onChange={onChange} />;
        break;
      default:
        cellContents = setCellContents(row, col.field);
        break;
    }

    const cls = classNames({
      'row-head-left': col.field == 'selectColumn',
      'row-head-right': col.field == 'toggleColumns',
      'id' : col.field == 'id',
      'name': col.field == 'name',
      'storefront-name' : col.field == 'storefrontName',
      'state' : col.field == 'state',
      'retail-price-field' : col.field == 'retailPrice',
      'sale-price-field' : col.field == 'salePrice',
      'coupon-code' : col.field == 'code',
      'total-uses' : col.field == 'totalUsed',
      'current-carts' : col.field == 'currentCarts',
      'line-item-sku': col.field == 'sku',
      'line-item-price': col.field == 'price',
      'line-item-quantity': col.field == 'quantity',
      'line-item-total-price': col.field == 'totalPrice',
      'coupon-codes' : col.field == 'codes',
      'email' : col.field == 'email',
      'ship-region' : col.field == 'shipRegion',
      'bill-region' : col.field == 'billRegion',
      'rank' : col.field == 'rank'
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
    checked: PropTypes.bool.isRequired,
    setChecked: PropTypes.func.isRequired,
  }),
};

MultiSelectRow.contextTypes = {
  router: PropTypes.object.isRequired,
};

MultiSelectRow.defaultProps = {
  onClick: _.noop
};

export default MultiSelectRow;
