/* @flow */

import React from 'react';

// libs
import _ from 'lodash';

// components
import ExpandableRow from 'components/table/expandable-row';

import type { WarehouseInventorySummary } from 'modules/inventory/warehouses';

type Props = {
  warehouse: WarehouseInventorySummary,
  columns: Columns,
  params: Object,
}

const InventoryWarehouseRow = (props: Props) => {
  const { warehouse, columns, params } = props;
  const key = `inventory-list-${warehouse.stockLocation.id}`;

  const setCellContents = (warehouse: WarehouseInventorySummary, field: string, params: Object) => {
    return _.get(warehouse, field);
  };

  return (
    <ExpandableRow
      key={key}
      columns={columns}
      row={warehouse}
      params={params}
      setCellContents={setCellContents}
    />
  );
};

export default InventoryWarehouseRow;
