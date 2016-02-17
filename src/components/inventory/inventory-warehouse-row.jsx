
// libs
import React, { PropTypes } from 'react';
import _ from 'lodash';
import { transitionTo } from '../../route-helpers';

// components
import ExpandableRow from '../table/expandable-row';
import Table from '../table/table';

const setCellContents = (warehouse, field, params) => {
  return _.get(warehouse, field);
};

const setDrawerContent = (row, params) => {
  return (
    <div>
      <Table
        columns={params.drawerColumns}
        data={params.drawerData} />
    </div>
  );
};

const InventoryWarehouseRow = props => {
  const { warehouse, columns, params } = props;
  const key = `inventory-list-${warehouse.id}`;

  return (
    <ExpandableRow
      cellKeyPrefix={key}
      columns={columns}
      onClick={_.noop}
      row={warehouse}
      params={params}
      setCellContents={setCellContents}
      setDrawerContent={setDrawerContent} />
  );
};

InventoryWarehouseRow.propTypes = {
  warehouse: PropTypes.object.isRequired,
  columns: PropTypes.array,
  params: PropTypes.object,
};

export default InventoryWarehouseRow;
