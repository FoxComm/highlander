
// libs
import React, { PropTypes } from 'react';
import _ from 'lodash';
import { transitionTo } from 'browserHistory';

// components
import MultiSelectRow from '../table/multi-select-row';

const setCellContents = (sku, field) => {
  if (field === 'productActive' || field === 'skuActive') {
    return _.get(sku, field) ? 'Active' : 'Inactive';
  }
  return _.get(sku, field);
};

const InventoryListRow = (props) => {
  const { sku, columns, params } = props;
  const key = `inventory-list-${sku.id}`;
  const clickAction = () => {
    transitionTo('inventory-item-details', { code: sku.code });
  };

  return (
    <MultiSelectRow
      cellKeyPrefix={key}
      columns={columns}
      onClick={clickAction}
      row={sku}
      setCellContents={setCellContents}
      params={params} />
  );
};

InventoryListRow.propTypes = {
  sku: PropTypes.object.isRequired,
  columns: PropTypes.array,
  params: PropTypes.object.isRequired,
};

export default InventoryListRow;
