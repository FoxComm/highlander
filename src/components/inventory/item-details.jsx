// @flow weak

import _ from 'lodash';
import React, { Component } from 'react';
import { haveType } from '../../modules/state-helpers';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// components
import ExpandableTable from '../table/expandable-table';
import InventoryWarehouseRow from './inventory-warehouse-row';
import WarehouseDrawer from './inventory-warehouse-drawer';

// redux
import * as WarehousesActions from 'modules/inventory/warehouses';
import type { InventorySummary, StockLocation } from 'modules/inventory/warehouses';

const mapStateToProps = (state, props) => ({
  inventoryDetails: _.get(state, ['inventory', 'warehouses', props.params.skuCode], {}),
  fetchState: _.get(state, 'asyncActions.inventory-summary', {}),
});

type Props = {
  inventoryDetails: Array<InventorySummary>,
  params: Object,
  fetchSummary: (skuCode: string) => Promise,
  fetchState: {
    inProgress?: boolean,
    err?: any,
  }
}

function array2tableData(rows) {
  return {
    rows,
    total: rows.length,
    from: 0,
    size: rows.length,
  };
}

class InventoryItemDetails extends Component {
  props: Props;

  componentDidMount() {
    this.props.fetchSummary(this.props.params.skuCode);
  }

  get tableColumns() {
    return [
      {field: 'stockLocationName', text: 'Warehouse'},
      {field: 'onHand', text: 'On Hand'},
      {field: 'onHold', text: 'Hold'},
      {field: 'reserved', text: 'Reserved'},
      {field: 'afs', text: 'AFS'},
      {field: 'afsCost', text: 'AFS Cost Value', type: 'currency'},
    ];
  }

  get drawerColumns() {
    return [
      {field: 'type', text: 'Type'},
      {field: 'onHand', text: 'On Hand'},
      {field: 'onHold', text: 'Hold'},
      {field: 'reserved', text: 'Reserved'},
      {field: 'afs', text: 'AFS'},
      {field: 'afsCost', text: 'AFS Cost Value', type: 'currency'},
    ];
  }

  @autobind
  renderDrawer(row, index, params) {
    const key = `inventory-warehouse-drawer-${row.id}`;
    return (
      <WarehouseDrawer
        key={key}
        data={this.drawerData(row)}
        columns={this.drawerColumns}
        isLoading={_.get(this.props, ['fetchState', 'inProgress'], true)}
        failed={!!_.get(this.props, ['fetchState', 'err'])}
        params={params}
      />
    );
  }

  @autobind
  renderRow(row, index, columns, params) {
    const key = `inventory-warehouse-row-${row.id}`;
    return (
      <InventoryWarehouseRow
        key={key}
        warehouse={row}
        columns={columns}
        params={params}
      />
    );
  }

  get summaryData() {
    const { inventoryDetails } = this.props;

    return array2tableData(_.map(inventoryDetails, details => details.stockLocation));
  }

  drawerData(stockLocation: StockLocation) {
    const inventoryDetails: Array<InventorySummary> = this.props.inventoryDetails;

    const summary: InventorySummary = _.find(inventoryDetails, (item: InventorySummary) => {
      return item.stockLocation.stockLocationId == stockLocation.stockLocationId;
    });
    return array2tableData(summary ? summary.stockItems : []);
  }

  render() {
    const isFetching = this.props.fetchState.inProgress !== false;
    const failed = !!this.props.fetchState.err;

    return (
      <div className="fc-grid">
        <div className="fc-col-md-1-1">
          <ExpandableTable
            columns={this.tableColumns}
            data={this.summaryData}
            renderRow={this.renderRow}
            renderDrawer={this.renderDrawer}
            idField="stockLocationId"
            isLoading={isFetching}
            failed={failed}
            emptyMessage="No warehouse data found."
            className="fc-inventory-item-details__warehouses-table"
          />
        </div>
      </div>
    );
  }
}

export default connect(mapStateToProps, WarehousesActions)(InventoryItemDetails);
