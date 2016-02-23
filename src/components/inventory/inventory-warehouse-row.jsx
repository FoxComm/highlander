
// libs
import React, { PropTypes } from 'react';
import _ from 'lodash';

// components
import ExpandableRow from '../table/expandable-row';
import Table from '../table/table';
import WarehouseDrawer from './inventory-warehouse-drawer';

const setCellContents = (warehouse, field, params) => {
  return _.get(warehouse, field);
};

class InventoryWarehouseRow extends React.Component {

  render() {
    const { warehouse, columns, params } = this.props;
    const key = `inventory-list-${warehouse.id}`;

    return (
      <ExpandableRow
        key={key}
        cellKeyPrefix={key}
        columns={columns}
        row={warehouse}
        params={params}
        setCellContents={setCellContents} />
    );
  }
};

InventoryWarehouseRow.propTypes = {
  warehouse: PropTypes.object.isRequired,
  columns: PropTypes.array.isRequired,
  params: PropTypes.object.isRequired,
};

export default InventoryWarehouseRow;
