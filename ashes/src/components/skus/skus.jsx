/* @flow */

import React, { Component, Element } from 'react';

// libs
import _ from 'lodash';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { bulkExportBulkAction, renderExportModal, getIdsByProps } from 'modules/bulk-export/helpers';
import { filterArchived } from 'elastic/archive';

// actions
import { actions } from 'modules/skus/list';
import { bulkExport } from 'modules/bulk-export/bulk-export';
import { actions as bulkActions } from 'modules/skus/bulk';

// components
import SelectableSearchList from '../list-page/selectable-search-list';
import SkuRow from './sku-row';
import BulkActions from 'components/bulk-actions/bulk-actions';
import BulkMessages from 'components/bulk-actions/bulk-messages';
import { Link } from 'components/link';

type Props = {
  actions: Object,
  list: Object,
  bulkExportAction: (
    fields: Array<string>, entity: string, identifier: string, description: string, sort: Array<Object>
  ) => Promise<*>,
  bulkActions: {
    exportByIds: (
      ids: Array<number>, description: string, fields: Array<Object>, entity: string, identifier: string
    ) => void,
  },
};

const tableColumns: Columns = [
  { field: 'skuCode', text: 'SKU' },
  { field: 'title', text: 'Title' },
  { field: 'salePrice', currencyField: 'salePriceCurrency', text: 'Sale Price', type: 'currency' },
  { field: 'retailPrice', currencyField: 'retailPriceCurrency', text: 'Retail Price', type: 'currency' }
];

export class Skus extends Component {
  props: Props;

  @autobind
  addSearchFilters(filters: Array<SearchFilter>, initial: boolean = false) {
    return this.props.actions.addSearchFilters(filterArchived(filters), initial);
  }

  renderRow(row: SkuSearchItem, index: number, columns: Columns, params: Object) {
    const key = `skus-${row.id}`;
    return (
      <SkuRow
        key={key}
        sku={row}
        columns={columns}
        params={params}
      />
    );
  }

  @autobind
  bulkExport(allChecked: boolean, toggledIds: Array<string>) {
    const { list } = this.props;
    const { exportByIds } = this.props.bulkActions;
    const results = list.currentSearch().results.rows;
    const ids = getIdsByProps('skuCode', toggledIds, results);
    const modalTitle = 'SKUs';
    const entity = 'skus';

    return renderExportModal(tableColumns, entity, modalTitle, exportByIds, ids);
  }

  get bulkActions(): Array<any> {
    return [
      bulkExportBulkAction(this.bulkExport, 'SKUs'),
    ];
  }

  @autobind
  renderBulkDetails(message: string, skuCode: number) {
    return (
      <span key={skuCode}>
        SKU <Link to="sku-details" params={{ skuCode }}>{skuCode}</Link>
      </span>
    );
  }

  render() {
    const { list, actions } = this.props;

    const searchActions = {
      ...actions,
      addSearchFilters: this.addSearchFilters,
    };

    return (
      <div>
        <BulkMessages
          storePath="skus.bulk"
          module="skus"
          entity="SKU"
          renderDetail={this.renderBulkDetails}
        />
        <BulkActions
          module="skus"
          entity="SKU"
          actions={this.bulkActions}
        >
          <SelectableSearchList
            exportEntity="skus"
            exportTitle="SKUs"
            bulkExport
            bulkExportAction={this.props.bulkExportAction}
            entity="skus.list"
            emptyMessage="No SKUs found."
            list={list}
            renderRow={this.renderRow}
            tableColumns={tableColumns}
            searchActions={searchActions}
            predicate={({skuCode}) => skuCode}
          />
        </BulkActions>
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    list: _.get(state.skus, 'list', {}),
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkExportAction: bindActionCreators(bulkExport, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(Skus);
