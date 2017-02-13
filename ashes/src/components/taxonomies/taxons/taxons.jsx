// @flow

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// data
import { actions } from 'modules/taxons/list';

// components
import MultiSelectTable from 'components/table/multi-select-table';
import SelectableSearchList from 'components/list-page/selectable-search-list';
import TaxonRow from './taxon-row';

// helpers
import { filterArchived } from 'elastic/archive';

// types
import type { TaxonResult } from 'paragons/taxon';
import type { SearchFilter } from 'elastic/common';

// styling
import styles from './taxons.css';

type Column = {
  field: string,
  text: string,
  type: ?string,
};

type Props = {
  actions: Object,
  list: Object,
};

type Props = {
  params: {
    taxonomyId: number,
  },
};

const tableColumns = [
  { field: 'name', text: 'Value Name' },
  { field: 'taxonId', text: 'ID' },
  { field: 'productsCount', text: 'Products' },
  { field: 'state', text: 'State' },
];

export class TaxonsListPage extends Component {
  props: Props;

  componentDidMount() {
    this.props.actions.fetch();
  }

  renderRow(row: TaxonResult, index: number, columns: Array<Column>, params: Object) {
    const key = `taxons-${row.id}`;
    return <TaxonRow key={key} taxon={row} columns={columns} params={params} />;
  }

  render(): Element {
    const { list, actions } = this.props;

    const results = list.currentSearch().results;

    return (
      <div styleName="container">
        <MultiSelectTable
          columns={tableColumns}
          data={results}
          renderRow={this.renderRow}
          setState={actions.updateStateAndFetch}
          predicate={({id}) => id}
          hasActionsColumn={false}
          isLoading={results.isFetching}
          failed={results.failed}
          emptyMessage={"No taxons found."}
          key={list.currentSearch().title} />
      </div>
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
