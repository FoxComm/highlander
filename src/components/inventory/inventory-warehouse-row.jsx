
// libs
import React, { PropTypes } from 'react';
import _ from 'lodash';
import { transitionTo } from '../../route-helpers';

// components
import ExpandableRow from '../table/expandable-row';

const setCellContents = (warehouse, field) => {
  return _.get(warehouse, field);
};

const InventoryWarehouseRow = props => {
  const { warehouse, columns } = props;
  const key = `inventory-list-${warehouse.id}`;

  return (
    <ExpandableRow
      cellKeyPrefix={key}
      columns={columns}
      onClick={_.noop}
      row={warehouse}
      setCellContents={setCellContents} />
  );
};

InventoryWarehouseRow.propTypes = {
  warehouse: PropTypes.object.isRequired,
  columns: PropTypes.array,
};

export default InventoryWarehouseRow;
