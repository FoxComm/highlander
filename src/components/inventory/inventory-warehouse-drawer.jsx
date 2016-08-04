// @flow
// libs
import React, { Element, Component } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

// components
import Table from '../table/table';
import Drawer from '../table/drawer';
import TableRow from '../table/row';
import AdjustQuantity from '../forms/adjust-quantity';

import * as WarehousesActions from 'modules/inventory/warehouses';

import type { StockItem } from 'modules/inventory/warehouses';

type State = {
  popupOpenedFor: number|null,
}

type Props = {
  updateSkuItemsCount: (stockItemId: number, diff: number) => void,
}

class WarehouseDrawer extends Component {
  props: Props;

  state: State = {
    popupOpenedFor: null,
  };

  togglePopupFor(id: number, show: boolean): void {
    this.setState({
      popupOpenedFor: show ? id : null,
    });
  }

  @autobind
  renderRow(row: StockItem): Element {
    const { state } = this;

    const handleChangeQuantity = (diff: number) => {
      this.props.updateSkuItemsCount(row.sku, row, diff);
    };

    return (
      <TableRow>
        <td>{row.type}</td>
        <td>
          <AdjustQuantity
            value={row.onHand}
            onChange={handleChangeQuantity}
            isPopupShown={state.popupOpenedFor === row.stockItemId}
            togglePopup={(show) => this.togglePopupFor(row.stockItemId, show)}
          />
        </td>
        <td>{row.onHold}</td>
        <td>{row.reserved}</td>
        <td>{row.afs}</td>
        <td>{row.afsCost}</td>
      </TableRow>
    );
  }

  render() {
    const { props } = this;
    return (
      <Drawer {...props} >
        <div>
          <Table
            {...props}
            className="fc-inventory-item-details__warehouse-details-table"
            renderRow={this.renderRow}
            emptyMessage="No warehouse data found."
          />
        </div>
      </Drawer>
    );
  }
}

export default connect(void 0, WarehousesActions)(WarehouseDrawer);
