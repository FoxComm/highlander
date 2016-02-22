
// libs
import React, { PropTypes } from 'react';
import _ from 'lodash';
import { transitionTo } from '../../route-helpers';

// components
import MultiSelectRow from '../table/multi-select-row';

const setCellContents = (sku, field) => {
  if (field === 'productActive' || field === 'skuActive') {
    return _.get(sku, field) ? 'Active' : 'Inactive';
  }
  return _.get(sku, field);
};

const InventoryListRow = (props, context) => {
  const { sku, columns, params } = props;
  const key = `inventory-list-${sku.id}`;
  const clickAction = () => {
    transitionTo(context.history, 'inventory-item-details', { sku: sku.code });
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

InventoryListRow.contextTypes = {
  history: PropTypes.object.isRequired
};

export default InventoryListRow;
