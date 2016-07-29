
// libs
import React, { PropTypes } from 'react';
import _ from 'lodash';

// components
import ExpandableRow from '../table/expandable-row';

const setCellContents = (warehouse, field, params) => {
  return _.get(warehouse, field);
};

const InventoryWarehouseRow = props => {
  const { warehouse, columns, params } = props;
  const key = `inventory-list-${warehouse.id}`;

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

InventoryWarehouseRow.propTypes = {
  warehouse: PropTypes.object.isRequired,
  columns: PropTypes.array.isRequired,
  params: PropTypes.object.isRequired,
};

export default InventoryWarehouseRow;
