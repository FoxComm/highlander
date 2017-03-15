// @flow

// libs
import React, { Component, Element } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// data
import { actions } from 'modules/taxons/list';

// components
import MultiSelectTable from 'components/table/multi-select-table';
import { AddButton } from 'components/common/buttons';
import TaxonRow from './taxon-row';

// helpers
import * as dsl from 'elastic/dsl';
import { transitionToLazy } from 'browserHistory';

// styling
import styles from './taxons.css';

import type { TaxonomyParams } from '../taxonomy';

type Column = {
  field: string,
  text: string,
  type: ?string,
};

type Props = ObjectPageChildProps<Taxonomy> & {
  actions: Object,
  list: Object,
  params: TaxonomyParams,
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

  renderRow(row: TaxonResult, index: number, columns: Array<Column>, params: Object) {
    return <TaxonRow key={row.id} taxon={row} columns={columns} params={params} />;
  }

  get tableControls(): Array<Element<*>> {
    const handleClick = transitionToLazy('value-details', { ...this.props.params, taxonId: 'new' });

    return [
      <AddButton className="fc-btn-primary" onClick={handleClick}>Value</AddButton>
    ];
  }

  render() {
    const { list, actions } = this.props;

    const results = list.currentSearch().results;

    return (
      <MultiSelectTable
        columns={tableColumns}
        data={results}
        renderRow={this.renderRow}
        setState={actions.updateStateAndFetch}
        predicate={({id}) => id}
        hasActionsColumn={false}
        isLoading={results.isFetching}
        failed={results.failed}
        emptyMessage={'This taxonomy does not have any values yet.'}
        headerControls={this.tableControls}
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
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(TaxonsListPage);
