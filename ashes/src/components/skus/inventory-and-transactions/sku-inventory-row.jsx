
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

// components
import ExpandableRow from 'components/table/expandable-row';

import type { WarehouseInventorySummary } from 'modules/inventory/warehouses';

const setCellContents = (warehouse, field, params) => {
  return _.get(warehouse, field);
};

type Props = {
  warehouse: WarehouseInventorySummary,
  columns: Array<any>,
  params: Object,
}

const InventoryWarehouseRow = (props: Props) => {
  const { warehouse, columns, params } = props;
  const key = `inventory-list-${warehouse.stockLocation.id}`;

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
