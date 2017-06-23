// libs
import _ from 'lodash';
import classNames from 'classnames';
import React from 'react';
import PropTypes from 'prop-types';

// components
import { Link } from 'components/link';
import columnPropType from './column-prop-type';
import { Date, DateTime, Time } from 'components/utils/datetime';
import Currency from 'components/utils/currency';
import State from '../common/state';
import Change from 'components/utils/change';
import ProductImage from 'components/imgix/product-image';

function getCurrency(column, row) {
  const currencyField = column.currencyField;
  return currencyField ? _.get(row, currencyField) : void 0;
}

function getCell(column, children, row) {
  const type = _.get(column, 'type');
  switch (type) {
    case 'id':
      return <Link to={column.model} params={{ [column.model]: children }}>{children}</Link>;
    case 'image':
      return <ProductImage src={children} width={50} height={50} />;
    case 'state':
      return <State value={children} model={column.model} />;
    case 'currency':
      return <Currency value={children} currency={getCurrency(column, row)} />;
    case 'transaction':
      return <Currency value={children} currency={getCurrency(column, row)} isTransaction={true} />;
    case 'date':
      return <Date value={children} />;
    case 'datetime':
      return <DateTime value={children} />;
    case 'time':
      return <Time value={children} />;
    case 'change':
      return <Change value={children} />;
    case 'raw':
      return <div dangerouslySetInnerHTML={{ __html: children }} />;
    default:
      if (column && column.render) {
        return column.render(children, row);
      }
      return children;
  }
}

const TableBodyCell = props => {
  const { row, children, colSpan, column, className, ...rest } = props;

  const cell = _.isNull(children) ? null : getCell(column, children, row);

  return <td className={classNames('fc-table-td', className)} colSpan={colSpan} {...rest}>{cell}</td>;
};

TableBodyCell.propTypes = {
  colSpan: PropTypes.number,
  column: columnPropType,
  children: PropTypes.node,
  className: PropTypes.string,
};

export default TableBodyCell;
