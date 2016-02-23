
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
          className="fc-inventory-item-details__warehouse-details-table"
          columns={drawerColumns}
          data={drawerData(row.id)}
          emptyMessage="No warehouse data found." />
      </div>
    </Drawer>
  );
};

export default WarehouseDrawer;
