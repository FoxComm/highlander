/** Libs */
import { get, isString, capitalize } from 'lodash';
import React, { PropTypes } from 'react';

/** Components */
import Row from 'components/table/row';
import Cell from 'components/table/cell';

/** InventoryItemTransactionsRow Component */
const InventoryItemTransactionsRow = props => {
  const { transaction, columns } = props;

  return (
    <Row>
      {columns.map(column => {
        return <Cell column={column} key={column.field}>{get(transaction, column.field)}</Cell>;
      })}
    </Row>
  );
};

/** InventoryItemTransactionsRow expected props types */
InventoryItemTransactionsRow.propTypes = {
  transaction: PropTypes.object.isRequired,
  columns: PropTypes.array.isRequired,
};

export default InventoryItemTransactionsRow;
