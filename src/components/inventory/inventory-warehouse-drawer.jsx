// @flow
// libs
import React, { Element } from 'react';

// components
import Table from '../table/table';
import Drawer from '../table/drawer';
import TableRow from '../table/row';
import AdjustQuantity from '../forms/adjust-quantity';

import type { StockItem } from 'modules/inventory/warehouses';


function renderRow(row: StockItem): Element {
  return (
    <TableRow>
      <td>{row.type}</td>
      <td>
        <AdjustQuantity
          value={row.onHand}
          onChange={newQuantity => {console.info('change', newQuantity)}}
        />
      </td>
      <td>{row.onHold}</td>
      <td>{row.reserved}</td>
      <td>{row.afs}</td>
      <td>{row.afsCost}</td>
    </TableRow>
  );
}

const WarehouseDrawer = (props: Object) => {
  return (
    <Drawer {...props} >
      <div>
        <Table
          {...props}
          className="fc-inventory-item-details__warehouse-details-table"
          renderRow={renderRow}
          emptyMessage="No warehouse data found."
        />
      </div>
    </Drawer>
  );
};

export default WarehouseDrawer;
