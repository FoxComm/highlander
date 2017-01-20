
// libs
import React, { Element, Component } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

// components
import Table from 'components/table/table';
import Drawer from 'components/table/drawer';
import TableRow from 'components/table/row';
import AdjustQuantity from 'components/forms/adjust-quantity';
import Currency from 'components/common/currency';

import * as WarehousesActions from 'modules/inventory/warehouses';

import type { StockItemFlat } from 'modules/inventory/warehouses';

type State = {
  popupOpenedFor: string|null,
}

type Props = {
  updateSkuItemsCount: (sku: string, stockItem: StockItemFlat, diff: number) => void,
  data: {
    rows: Array<StockItemFlat>
  },
}

class WarehouseDrawer extends Component {
  props: Props;

  state: State = {
    popupOpenedFor: null,
  };

  togglePopupFor(id: string, show: boolean): void {
    this.setState({
      popupOpenedFor: show ? id : null,
    });
  }

  @autobind
  renderRow(row: StockItemFlat): Element {
    const { state } = this;

    const handleChangeQuantity = (diff: number) => {
      this.props.updateSkuItemsCount(row.sku, row, diff);
    };

    const uniqId = `${row.type}-${row.id}`;

    return (
      <TableRow key={uniqId}>
        <td>{row.type}</td>
        <td>
          <AdjustQuantity
            value={row.onHand}
            onChange={handleChangeQuantity}
            isPopupShown={state.popupOpenedFor === uniqId}
            togglePopup={(show) => this.togglePopupFor(uniqId, show)}
          />
        </td>
        <td>{row.onHold}</td>
        <td>{row.reserved}</td>
        <td>{row.afs}</td>
        <td><Currency value={row.afsCost}/></td>
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
