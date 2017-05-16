/* @flow */

// libs
import React from 'react';
import _ from 'lodash';

// components
import MultiSelectRow from '../table/multi-select-row';

type Props = {
  columns: Array<Object>,
  sku: Object,
  params: Object,
};

const InventoryListRow = (props: Props) => {
  const { sku, columns, params } = props;

  const setCellContents = (sku, field) => {
    return _.get(sku, field);
  };

  return (
    <MultiSelectRow
      columns={columns}
      linkTo="sku-inventory-details"
      linkParams={{skuCode: sku.sku}}
      row={sku}
      setCellContents={setCellContents}
      params={params}
    />
  );
};

export default InventoryListRow;
