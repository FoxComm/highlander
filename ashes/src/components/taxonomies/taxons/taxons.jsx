// @flow

import React, { Component, Element } from 'react';

// libs
import _ from 'lodash';
import classNames from 'classnames';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import * as dsl from 'elastic/dsl';
import { transitionToLazy } from 'browserHistory';
import { bulkExportBulkAction, renderExportModal } from 'modules/bulk-export/helpers';

// components
import MultiSelectTable from 'components/table/multi-select-table';
import TreeTable from 'components/table/tree-table';
import { AddButton } from 'components/core/button';
import TaxonRow from './taxon-row';
import BulkActions from 'components/bulk-actions/bulk-actions';
import BulkMessages from 'components/bulk-actions/bulk-messages';
import { Link } from 'components/link';

// actions
import { actions } from 'modules/taxons/list';
import { bulkExport } from 'modules/bulk-export/bulk-export';
import { actions as bulkActions } from 'modules/taxons/bulk';

import type { TaxonomyParams } from '../taxonomy';

import styles from './taxons.css';

type Props = ObjectPageChildProps<Taxonomy> & {
  taxonomy: Taxonomy,
  actions: Object,
  list: Object,
  params: TaxonomyParams,
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
  { field: 'name', text: 'Value Name' },
  { field: 'taxonId', text: 'ID' },
  { field: 'productsCount', text: 'Products' },
  { field: 'createdAt', type: 'datetime', text: 'Date/Time Created' },
  { field: 'updatedAt', type: 'datetime', text: 'Date/Time Updated' },
  { field: 'state', text: 'State' },
];

export class TaxonsListPage extends Component {
  props: Props;

  componentDidMount() {
    this.props.actions.setExtraFilters([
      dsl.existsFilter('archivedAt', 'missing'),
      dsl.termFilter('taxonomyId', this.props.object.id)
    ]);
    this.props.actions.fetch();
  }

  renderRow(row: TaxonResult, index: number, columns: Columns, params: Object) {
    const key = `taxon-list-${row.id}`;

    return (
      <TaxonRow
        key={key}
        taxon={row}
        columns={columns}
        params={params}
      />
    );
  }

  get tableControls(): Array<Element<*>> {
    const handleClick = transitionToLazy('taxon-details', { ...this.props.params, taxonId: 'new' });

    return [
      <AddButton
        className={classNames('fc-btn-primary', styles.headerButton)}
        onClick={handleClick}
      >
        Value
      </AddButton>
    ];
  }

  @autobind
  bulkExport(allChecked: boolean, toggledIds: Array<number>) {
    const { exportByIds } = this.props.bulkActions;
    const modalTitle = 'Taxons';
    const entity = 'taxons';

    return renderExportModal(tableColumns, entity, modalTitle, exportByIds, toggledIds);
  }

  get bulkActions(): Array<any> {
    return [
      bulkExportBulkAction(this.bulkExport, 'Taxons'),
    ];
  }

  @autobind
  renderBulkDetails(context: string, taxonId: string) {
    const taxonomyId = this.props.object.id;

    return (
      <span key={taxonId}>
        Taxon <Link to="taxon-details" params={{ taxonId, taxonomyId, context }}>{taxonId}</Link>
      </span>
    );
  }

  render() {
    const { taxonomy, list } = this.props;

    const results = list.currentSearch().results;

    const Table = taxonomy.hierarchical ? TreeTable : MultiSelectTable;

    return (
      <div>
        <BulkMessages
          storePath="taxons.bulk"
          module="taxons"
          entity="taxon"
          renderDetail={this.renderBulkDetails}
          className={styles['bulk-message']}
        />
        <BulkActions
          module="taxons"
          entity="taxon"
          actions={this.bulkActions}
        >
          <Table
            exportEntity="taxons"
            exportTitle="Taxons"
            bulkExport
            bulkExportAction={this.props.bulkExportAction}
            columns={tableColumns}
            data={results}
            renderRow={this.renderRow}
            hasActionsColumn={false}
            isLoading={results.isFetching}
            failed={results.failed}
            emptyMessage={'This taxonomy does not have any values yet.'}
            headerControls={this.tableControls}
            idField="taxonId"
            className={styles.taxonsTable}
          />
        </BulkActions>
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    list: _.get(state.taxons, 'list', {}),
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkExportAction: bindActionCreators(bulkExport, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(TaxonsListPage);
