/* @flow */

import React, { Component } from 'react';

// libs
import _ from 'lodash';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { autobind } from 'core-decorators';
import { bulkExportBulkAction, renderExportModal } from 'modules/bulk-export/helpers';

// actions
import { actions } from 'modules/inventory/list';
import { bulkExport } from 'modules/bulk-export/bulk-export';
import { actions as bulkActions } from 'modules/inventory/bulk';

// component
import { SelectableSearchList } from 'components/list-page';
import InventoryListRow from './inventory-list-row';
import BulkActions from 'components/bulk-actions/bulk-actions';
import BulkMessages from 'components/bulk-actions/bulk-messages';
import { Link } from 'components/link';

type Props = {
  actions: Object,
  list: Object,
  bulkExportAction: (
    fields: Array<string>, entity: string, identifier: string, description: string
  ) => Promise<*>,
  bulkActions: {
    exportByIds: (
      ids: Array<number>, description: string, fields: Array<Object>, entity: string, identifier: string
    ) => void,
  },
};

const tableColumns = [
  { field: 'sku', text: 'SKU' },
  { field: 'type', text: 'SKU Type' },
  { field: 'stockLocation.name', text: 'Warehouse' },
  { field: 'onHand', text: 'On Hand' },
  { field: 'onHold', text: 'Hold' },
  { field: 'reserved', text: 'Reserved' },
  { field: 'afs', text: 'AFS' },
  { field: 'afsCost', text: 'AFS Cost', type: 'currency' },
];

class InventoryList extends Component {
  props: Props;

  @autobind
  renderRow(row: Object, index: number, columns: Columns, params: Object) {
    const key = `inventory-sku-${row.id}`;

    return (
      <InventoryListRow
        sku={row}
        columns={columns}
        params={params}
        key={key}
      />
    );
  }

  @autobind
  bulkExport(allChecked: boolean, toggledIds: Array<number>) {
    const { exportByIds } = this.props.bulkActions;
    const modalTitle = 'Inventory Units';
    const entity = 'inventory';

    return renderExportModal(tableColumns, entity, modalTitle, exportByIds, toggledIds);
  }

  get bulkActions(): Array<any> {
    return [
      bulkExportBulkAction(this.bulkExport, 'Inventory Units'),
    ];
  }

  @autobind
  renderBulkDetails(message: string, skuCode: number) {
    return (
      <span key={skuCode}>
        Inventory Unit <Link to="sku-inventory-details" params={{ skuCode }}>{skuCode}</Link>
      </span>
    );
  }

  render() {
    return (
      <div>
        <BulkMessages
          storePath="inventory.bulk"
          module="inventory"
          entity="inventory unit"
          renderDetail={this.renderBulkDetails}
        />
        <BulkActions
          module="inventory"
          entity="inventory unit"
          actions={this.bulkActions}
        >
          <SelectableSearchList
            exportEntity="inventory"
            exportTitle="Inventory Units"
            bulkExport
            bulkExportAction={this.props.bulkExportAction}
            entity="inventory.list"
            emptyMessage="No inventory units found."
            list={this.props.list}
            renderRow={this.renderRow}
            tableColumns={tableColumns}
            searchActions={this.props.actions}
          />
        </BulkActions>
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    list: _.get(state.inventory, 'list', {}),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkExportAction: bindActionCreators(bulkExport, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(InventoryList);
