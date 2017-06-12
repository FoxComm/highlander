/* @flow */

// libs
import React, { Component, Element } from 'react';
import _ from 'lodash';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { bulkExportBulkAction, renderExportModal } from 'modules/bulk-export/helpers';

// actions
import { actions } from 'modules/catalog/list';
import { bulkExport } from 'modules/bulk-export/bulk-export';
import { actions as bulkActions } from 'modules/catalog/bulk';

// components
import SelectableSearchList from '../list-page/selectable-search-list';
import CatalogRow from './catalog-row';

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

const tableColumns: Columns = [
  { field: 'id', text: 'Catalog ID' },
  { field: 'name', text: 'Name' },
  { field: 'site', text: 'Site' },
  { field: 'countryName', text: 'Country' },
  { field: 'defaultLanguage', text: 'Language' },
  { field: 'updatedAt', text: 'Last Updated', type: 'datetime' },
];

export class CatalogsList extends Component {
  props: Props;

  get bulkActions(): Array<any> {
    return [bulkExportBulkAction(this.bulkExport, 'Catalogs')];
  }

  addSearchFilters = (filters: Array<SearchFilter>, initial: boolean = false) => {
    return this.props.actions.addSearchFilters(filters, initial);
  };

  bulkExport = (allChecked: boolean, toggledIds: Array<number>) => {
    const { exportByIds } = this.props.bulkActions;
    const modalTitle = 'Catalogs';
    const entity = 'catalogs';

    return renderExportModal(tableColumns, entity, modalTitle, exportByIds, toggledIds);
  };

  renderBulkDetails(id: number) {
    return (
      <span key={id}>
        Catalog <Link to="catalog-details" params={{ id }}>{id}</Link>
      </span>
    );
  }

  renderRow(row: Catalog, index: number, columns: Columns, params: Object) {
    const key = `catalogs-${_.get(row, 'id', index)}`;
    return (
      <CatalogRow
        key={key}
        catalog={row}
        columns={columns}
        params={params}
      />
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
          storePath="catalogs.bulk"
          module="catalogs"
          entity="catalog"
          renderDetail={this.renderBulkDetails}
        />
        <BulkActions
          module="catalogs"
          entity="catalog"
          actions={this.bulkActions}
        >
          <SelectableSearchList
            exportEntity="catalogs"
            exportTitle="Catalogs"
            bulkExport
            bulkExportAction={this.props.bulkExportAction}
            entity="catalogs.list"
            emptyMessage="No catalogs found."
            list={list}
            renderRow={this.renderRow}
            tableColumns={tableColumns}
            searchActions={searchActions}
            predicate={({id}) => id}
          />
        </BulkActions>
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    list: _.get(state.catalogs, 'list', {}),
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkExportAction: bindActionCreators(bulkExport, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(CatalogsList);
