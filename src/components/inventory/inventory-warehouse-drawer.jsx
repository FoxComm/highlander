
// libs
import React, { PropTypes } from 'react';

// components
import Table from '../table/table';
import Drawer from '../table/drawer';

const WarehouseDrawer = props => {
  const {drawerColumns, drawerData, row} = props;

  return (
    <Drawer {...props} >
      <div>
        <Table
          {...props}
          className="fc-inventory-item-details__warehouse-details-table"
          columns={drawerColumns}
          data={drawerData(row.id)}
          emptyMessage="No warehouse data found." />
      </div>
    </Drawer>
  );
};

WarehouseDrawer.propTypes = {
  row: PropTypes.object.isRequired,
  drawerColumns: PropTypes.array.isRequired,
  drawerData: PropTypes.func.isRequired,
  isLoading: PropTypes.bool,
  failed: PropTypes.bool,
};

export default WarehouseDrawer;
