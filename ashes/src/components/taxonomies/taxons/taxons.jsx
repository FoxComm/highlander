// @flow

// libs
import classNames from 'classnames';
import React, { Component, Element } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import * as dsl from 'elastic/dsl';
import { transitionToLazy } from 'browserHistory';

// components
import MultiSelectTable from 'components/table/multi-select-table';
import TreeTable from 'components/table/tree-table';
import { AddButton } from 'components/core/button';
import TaxonRow from './taxon-row';

// actions
import { actions } from 'modules/taxons/list';
import { bulkExport } from 'modules/bulk-export/bulk-export';

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
    return <TaxonRow key={row.id} taxon={row} columns={columns} params={params} />;
  }

  get tableControls(): Array<Element<*>> {
    const handleClick = transitionToLazy('taxon-details', { ...this.props.params, taxonId: 'new' });

    return [
      <AddButton className={classNames('fc-btn-primary', styles.headerButton)} onClick={handleClick}>Value</AddButton>
    ];
  }

  render() {
    const { taxonomy, list } = this.props;

    const results = list.currentSearch().results;

    const Table = taxonomy.hierarchical ? TreeTable : MultiSelectTable;

    return (
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
    );
  }
}

function mapStateToProps({ taxons: { list } }) {
  return { list };
}

function mapDispatchToProps(dispatch) {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkExportAction: bindActionCreators(bulkExport, dispatch),
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(TaxonsListPage);
