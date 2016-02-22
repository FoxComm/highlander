
//libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { haveType } from '../../modules/state-helpers';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// components
import { SectionTitle } from '../section-title';
import TabListView from '../tabs/tabs';
import TabView from '../tabs/tab';
import { Link, IndexLink } from '../link';
import ExpandableTable from '../table/expandable-table';
import InventoryWarehouseRow from './inventory-warehouse-row';

// redux
import * as WarehousesActions from '../../modules/inventory/warehouses';

const mapStateToProps = (state, props) => ({
  tableState: _.get(state, ['inventory', 'warehouses', props.params.sku], {})
});

@connect(mapStateToProps, {...WarehousesActions})
export default class InventoryItemDetails extends React.Component {

  static propTypes = {
    params: PropTypes.object,
  }

  componentDidMount() {
    this.props.fetchSummary(this.props.params.sku);
  }

  componentWillReceiveProps(nextProps) {
    const warehouses = _.get(nextProps, ['tableState', 'summary', 'results', 'rows'], []);
    _.each(warehouses, (wh) => {
      console.log(wh.id);
      this.props.fetchDetails(this.props.params.sku, wh.id);
    });
  }

  get tableColumns() {
    return [
      {field: 'name', text: 'Warehouse'},
      {field: 'onHand', text: 'On Hand'},
      {field: 'onHold', text: 'Hold'},
      {field: 'reserved', text: 'Reserved'},
      {field: 'safetyStock', text: 'Safety Stock'},
      {field: 'afs', text: 'AFS'},
      {field: 'afsCost', text: 'AFS Cost Value', type: 'currency'},
    ];
  }

  get drawerColumns() {
    return [
      {field: 'skuType', text: 'Type'},
      {field: 'onHand', text: 'On Hand'},
      {field: 'onHold', text: 'Hold'},
      {field: 'reserved', text: 'Reserved'},
      {field: 'safetyStock', text: 'Safety Stock'},
      {field: 'afs', text: 'AFS'},
      {field: 'afsCost', text: 'AFS Cost Value', type: 'currency'},
    ];
  }

  @autobind
  renderRow(row, index, columns, params) {
    const key = `inventory-warehouse-row-${row.id}`;
    return (
      <InventoryWarehouseRow
        warehouse={row}
        columns={columns}
        params={params}
        fetchDetails={(warehouseId) => this.props.fetchDetails(this.props.params.sku)} />
    );
  }

  get summaryData() {
    return _.get(this.props, ['tableState', 'summary', 'results'], {});
  }

  get drawerData() {
    return warehouseId => _.get(this.props, ['tableState', warehouseId, 'results'], {});
  }

  render() {
    console.log(this.props.tableState);
    const params = {
      drawerData: this.drawerData,
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
              data={this.summaryData}
              renderRow={this.renderRow}
              params={params}
              entity={haveType(this.props.params, 'inventoryItem')}
              idField="id"
              emptyMessage="No warehouse data found."
              className="fc-inventory-item-details__warehouses-table"/>
          </div>
        </div>
      </div>
    );
  }
}
