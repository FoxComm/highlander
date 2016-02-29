/** Libs */
import React, { PropTypes } from 'react';
import _ from 'lodash';

/** Components */
import TableView from '../table/tableview.jsx';

const setCellContents = (sku, field) => {
  if (field === 'productActive' || field === 'skuActive') {
    return _.get(sku, field) ? 'Active' : 'Inactive';
  }
  return _.get(sku, field);
};

const InventoryItemTransactionsListRow = props => {
  const {sku, columns, params} = props;
  const key = `inventory-item-transactions-list-${sku.id}`;

  return (
    <TableView
      cellKeyPrefix={key}
      columns={columns}
      row={sku}
      setCellContents={setCellContents}
      params={params}/>
  );
};

InventoryItemTransactionsListRow.propTypes = {
  sku: PropTypes.object.isRequired,
  columns: PropTypes.array,
  params: PropTypes.object.isRequired
};

export default InventoryItemTransactionsListRow;
