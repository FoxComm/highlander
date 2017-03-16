/* @flow */

import get from 'lodash/get';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { transitionToLazy } from 'browserHistory';

import { actions } from 'modules/taxons/details/products-list';

import { SectionTitle } from 'components/section-title';
import { AddButton } from 'components/common/buttons';
import SelectableSearchList from 'components/list-page/selectable-search-list';
import ProductRow from 'components/products/product-row';

import { makeTotalCounter } from 'components/list-page';

import * as dsl from 'elastic/dsl';
import { filterArchived } from 'elastic/archive';

import styles from './taxons.css';

import type { Product } from 'paragons/product';
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

const tableColumns: Array<Column> = [
  { field: 'productId', text: 'Product ID', type: null },
  { field: 'image', text: 'Image', type: 'image' },
  { field: 'title', text: 'Name', type: null },
  { field: 'state', text: 'State', type: null },
];

export class TaxonProductsPage extends Component {
  props: Props;

  componentDidMount() {
    this.props.actions.setExtraFilters([
      dsl.termFilter('taxonomies', this.props.object.id)
    ]);

    this.props.actions.fetch();
  }

  @autobind
  addSearchFilters(filters: Array<SearchFilter>, initial: boolean = false) {
    return this.props.actions.addSearchFilters(filterArchived(filters), initial);
  }

  renderRow(row: Product, index: number, columns: Array<Column>, params: Object) {
    return <ProductRow key={row.id} product={row} columns={columns} params={params} />;
  }

  render() {
    const { list, actions } = this.props;

    const searchActions = {
      ...actions,
      addSearchFilters: this.addSearchFilters,
    };

    const TotalCounter = makeTotalCounter(state => get(state, 'taxons.details.products'), actions);

    return (
      <div className="fc-products-list">
        <SectionTitle
          title="Products"
          subtitle={<TotalCounter />}
          addTitle="Product"
          onAddClick={transitionToLazy('taxon-details', this.props.params)}
        />

        <SelectableSearchList
          entity="taxons.details.products"
          emptyMessage="No products found."
          list={list}
          renderRow={this.renderRow}
          tableColumns={tableColumns}
          searchOptions={{ singleSearch: true }}
          searchActions={searchActions}
          predicate={({ id }) => id}
        />
      </div>
    );
  }
}

const mapState = state => ({
  list: get(state, 'taxons.details.products'),
});

const mapActions = dispatch => ({
  actions: bindActionCreators(actions, dispatch),
});

export default connect(mapState, mapActions)(TaxonProductsPage);
