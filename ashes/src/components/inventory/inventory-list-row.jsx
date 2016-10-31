
// libs
import React, { PropTypes } from 'react';
import _ from 'lodash';

// components
import MultiSelectRow from '../table/multi-select-row';

const setCellContents = (sku, field) => {
  return _.get(sku, field);
};

const InventoryListRow = (props) => {
  const { sku, columns, params } = props;

  return (
    <MultiSelectRow
      columns={columns}
      linkTo="sku-inventory-details"
      linkParams={{skuId: sku.skuId}}
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
