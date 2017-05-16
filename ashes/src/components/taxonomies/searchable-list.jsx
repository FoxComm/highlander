/* @flow */

import React, { Component, Element } from 'react';

// libs
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { filterArchived } from 'elastic/archive';
import { bulkExportBulkAction, renderExportModal } from 'modules/bulk-export/helpers';

// components
import SelectableSearchList from 'components/list-page/selectable-search-list';
import TaxonomyRow from './taxonomy-row';
import BulkActions from 'components/bulk-actions/bulk-actions';
import BulkMessages from 'components/bulk-actions/bulk-messages';
import { Link } from 'components/link';

// actions
import { actions } from 'modules/taxonomies/list';
import { bulkExport } from 'modules/bulk-export/bulk-export';
import { actions as bulkActions } from 'modules/taxonomies/bulk';

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
  { field: 'taxonomyId', text: 'ID' },
  { field: 'name', text: 'Name' },
  { field: 'type', text: 'Type' },
  { field: 'valuesCount', text: 'Values' },
  { field: 'createdAt', type: 'datetime', text: 'Date/Time Created' },
  { field: 'updatedAt', type: 'datetime', text: 'Date/Time Updated' },
  { field: 'state', text: 'State' },
];

export class SearchableList extends Component {
  props: Props;

  @autobind
  addSearchFilters(filters: Array<SearchFilter>, initial: boolean = false) {
    return this.props.actions.addSearchFilters(filterArchived(filters), initial);
  }

  renderRow(row: TaxonomyResult, index: number, columns: Columns, params: Object) {
    const key = `taxonomies-${row.id}`;

    return (
      <TaxonomyRow
        key={key}
        taxonomy={row}
        columns={columns}
        params={params}
      />
    );
  }

  @autobind
  bulkExport(allChecked: boolean, toggledIds: Array<number>) {
    const { exportByIds } = this.props.bulkActions;
    const modalTitle = 'Taxonomies';
    const entity = 'taxonomies';

    return renderExportModal(tableColumns, entity, modalTitle, exportByIds, toggledIds);
  }

  get bulkActions(): Array<any> {
    return [
      bulkExportBulkAction(this.bulkExport, 'Taxonomies'),
    ];
  }

  renderBulkDetails(context: string, taxonomyId: string) {
    return (
      <span key={taxonomyId}>
        Taxonomy <Link to="taxonomy-details" params={{ taxonomyId, context }}>{taxonomyId}</Link>
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
          storePath="taxonomies.bulk"
          module="taxonomies"
          entity="taxonomy"
          renderDetail={this.renderBulkDetails}
        />
        <BulkActions
          module="taxonomies"
          entity="taxonomy"
          actions={this.bulkActions}
        >
          <SelectableSearchList
            exportEntity="taxonomies"
            exportTitle="Taxonomies"
            bulkExport
            bulkExportAction={this.props.bulkExportAction}
            entity="taxonomies.list"
            emptyMessage="No taxonomies found."
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
    list: _.get(state.taxonomies, 'list', {}),
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkExportAction: bindActionCreators(bulkExport, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(SearchableList);
