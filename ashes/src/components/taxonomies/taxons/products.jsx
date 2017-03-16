/* @flow */

// libs
import get from 'lodash/get';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// actions
import { actions } from 'modules/taxons/details/products-list';

// components
import { SectionTitle } from 'components/section-title';
import { AddButton } from 'components/common/buttons';
import SelectableSearchList from 'components/list-page/selectable-search-list';
import ProductRow from 'components/products/product-row';
import { makeTotalCounter } from 'components/list-page';
import ProductsAddModal from './products-add-modal';

// helpers
import { transitionToLazy } from 'browserHistory';
import { filterArchived } from 'elastic/archive';
import * as dsl from 'elastic/dsl';

// styles
import styles from './taxons.css';

// types
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

type State = {
  modalVisible: boolean,
}

const tableColumns: Array<Column> = [
  { field: 'productId', text: 'ID' },
  { field: 'image', text: 'Image', type: 'image' },
  { field: 'title', text: 'Name' },
  { field: 'skus', text: 'Skus' },
  { field: 'state', text: 'State' },
];

export class TaxonProductsPage extends Component {
  props: Props;

  state: State = {
    modalVisible: false,
  };

  componentDidMount() {
    this.props.actions.setExtraFilters([
      dsl.termFilter('taxonomies', this.props.object.id)
    ]);

    this.props.actions.fetch();
  }

  @autobind
  openModal() {
    this.setState({ modalVisible: true });
  }

  @autobind
  closeModal() {
    this.setState({ modalVisible: false });
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
          onAddClick={this.openModal}
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
        <ProductsAddModal
          title="Products"
          isVisible={this.state.modalVisible}
          onCancel={this.closeModal}
          onConfirm={this.closeModal}
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
