
//libs
import React, { PropTypes } from 'react';

// components
import { SectionTitle } from '../section-title';
import TabListView from '../tabs/tabs';
import TabView from '../tabs/tab';
import { Link, IndexLink } from '../link';
import ExpandableTable from '../table/expandable-table';
import InventoryWarehouseRow from './inventory-warehouse-row';

export default class InventoryItemDetails extends React.Component {

  static propTypes = {
    params: PropTypes.object,
  }

  get tableColumns() {
    return [
      {field: 'warehouse', text: 'Warehouse'},
      {field: 'onHand', text: 'On Hand'},
      {field: 'onHold', text: 'Hold'},
      {field: 'reserved', text: 'Reserved'},
      {field: 'safetyStock', text: 'Safety Stock'},
      {field: 'afs', text: 'AFS'},
      {field: 'afsCostValue', text: 'AFS Cost Value', type: 'currency'},
    ];
  }

  get drawerColumns() {
    return [
      {field: 'type', text: 'Type'},
      {field: 'onHand', text: 'On Hand'},
      {field: 'onHold', text: 'Hold'},
      {field: 'reserved', text: 'Reserved'},
      {field: 'safetyStock', text: 'Safety Stock'},
      {field: 'afs', text: 'AFS'},
      {field: 'afsCostValue', text: 'AFS Cost Value', type: 'currency'},
    ];
  }

  // mocked data for table
  get mockedDrawerData() {
    return {
      rows:[
        {
          type: 'Sellable',
          onHand: 52,
          onHold: 2,
          reserved: 5,
          safetyStock: 3,
          afs: 42,
          afsCostValue: 42000,
        },
        {
          type: 'Preorder',
          onHand: 0,
          onHold: 0,
          reserved: 0,
          safetyStock: 0,
          afs: 0,
          afsCostValue: 0,
        },
        {
          type: 'Backorder',
          onHand: 0,
          onHold: 0,
          reserved: 0,
          safetyStock: 0,
          afs: 0,
          afsCostValue: 0,
        },
        {
          type: 'Non-sellable',
          onHand: 0,
          onHold: 0,
          reserved: 0,
          safetyStock: 0,
          afs: 0,
          afsCostValue: 0,
        },
      ], total:4, from:0, size:25
    };
  }

  get mockedData() {
    return {
      rows:[
        {
          warehouse: 'Colombus',
          onHand: 52,
          onHold: 2,
          reserved: 5,
          safetyStock: 3,
          afs: 42,
          afsCostValue: 42000,
        }
      ], total:1, from:0, size:25
    };
  }

  renderRow(row, index, columns, params) {
    const key = `inventory-warehouse-row-${row.warehouse}`;
    return <InventoryWarehouseRow warehouse={row} columns={columns} params={params} />;
  }

  render() {
    const params = {
      drawerData: this.mockedDrawerData,
      drawerColumns: this.drawerColumns,
    };
    return (
      <div className="fc-inventory-item-details">
        <div className="fc-inventory-item-details__summary">
          <div className="fc-grid">
            <div className="fc-col-md-1-1">
              <SectionTitle title="Inventory" />
            </div>
          </div>
          <TabListView>
            <TabView draggable={false} selected={true} >
              <IndexLink to="inventory-item-details"
                         params={this.props.params}
                         className="fc-inventory-item-details__tab-link">
                Inventory
              </IndexLink>
            </TabView>
            <TabView draggable={false} selected={false} >
              <Link to="inventory-item-transactions"
                    params={this.props.params}
                    className="fc-inventory-item-details__tab-link">
                Transactions
              </Link>
            </TabView>
          </TabListView>
        </div>
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            <ExpandableTable
              columns={this.tableColumns}
              data={this.mockedData}
              renderRow={this.renderRow}
              params={params}
              emptyMessage="No warehouse data found."
              className="fc-inventory-item-details__warehouses-table"/>
          </div>
        </div>
      </div>
    );
  }
}
