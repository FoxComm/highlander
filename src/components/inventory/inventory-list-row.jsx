
// libs
import React, { PropTypes } from 'react';
import _ from 'lodash';
import { transitionTo } from '../../route-helpers';

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
    console.log('Not implemented yet.');
    //transitionTo(context.history, 'giftcard', { giftCard: giftCard.code });
  };

  return (
    <MultiSelectRow
      cellKeyPrefix={key}
      columns={columns}
      onClick={clickAction}
      row={giftCard}
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
