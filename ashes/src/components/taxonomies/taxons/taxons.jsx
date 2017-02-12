// @flow

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// data
import { actions } from 'modules/taxons/list';

// components
import SelectableSearchList from 'components/list-page/selectable-search-list';
import TaxonRow from './taxon-row';

// helpers
import { filterArchived } from 'elastic/archive';

// types
import type { TaxonResult } from 'paragons/taxon';
import type { SearchFilter } from 'elastic/common';

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

  renderRow(row: TaxonResult, index: number, columns: Array<Column>, params: Object) {
    const key = `taxons-${row.id}`;
    return <TaxonRow key={key} taxon={row} columns={columns} params={params} />;
  }

  render(): Element {
    const { list, actions } = this.props;

    return (
      <div>
        <SelectableSearchList
          entity="taxons.list"
          emptyMessage="No taxons found."
          list={list}
          renderRow={this.renderRow}
          tableColumns={tableColumns}
          searchActions={actions}
          predicate={({id}) => id} />
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
