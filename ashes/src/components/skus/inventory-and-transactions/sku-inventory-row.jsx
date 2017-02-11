
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

// components
import { Link } from 'components/link';
import ExpandableRow from 'components/table/expandable-row';

import type { WarehouseInventorySummary } from 'modules/skus/warehouses';

const setCellContents = (skuId, warehouse, field, params) => {
  switch (field) {
    case 'sku':
      return <Link to="sku-details" params={{skuId: skuId}}>{warehouse.sku}</Link>;
    default:
      return _.get(warehouse, field);
  }
};

type Props = {
  warehouse: WarehouseInventorySummary,
  columns: Array<any>,
  params: Object,
  skuId: number,
}

const InventoryWarehouseRow = (props: Props) => {
  const { warehouse, columns, params, skuId } = props;
  const key = `inventory-list-${warehouse.stockLocation.id}`;

  return (
    <ExpandableRow
      key={key}
      columns={columns}
      row={warehouse}
      params={params}
      setCellContents={(...args) => setCellContents(skuId, ...args)}
    />
  );
};

export default InventoryWarehouseRow;
