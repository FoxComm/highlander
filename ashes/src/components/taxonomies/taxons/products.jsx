/* @flow */

// libs
import get from 'lodash/get';
import { autobind } from 'core-decorators';
import React, { Component, Element } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// actions
import { actions } from 'modules/taxons/details/products-list';
import { addProduct } from 'modules/taxons/details/taxon';

// components
import { SectionTitle } from 'components/section-title';
import { AddButton } from 'components/common/buttons';
import SelectableSearchList from 'components/list-page/selectable-search-list';
import ProductRow from 'components/products/product-row';
import { makeTotalCounter } from 'components/list-page';
import { ProductsAddModal } from 'components/product-add';

// helpers
import { filterArchived } from 'elastic/archive';
import * as dsl from 'elastic/dsl';

// styles
import styles from './taxons.css';

// types
import type { TaxonomyParams } from '../taxonomy';

type Props = ObjectPageChildProps<Taxon> & {
  actions: Object,
  list: Object,
  addState: AsyncState,
  params: TaxonomyParams & {
    taxonId: number,
  },
};

type State = {
  modalVisible: boolean,
}

const tableColumns: Columns = [
  { field: 'productId', text: 'ID' },
  { field: 'image', text: 'Image', type: 'image' },
  { field: 'title', text: 'Name' },
  { field: 'skus', text: 'SKUs' },
  { field: 'state', text: 'State' },
];

export class TaxonProductsPage extends Component {
  props: Props;

  state: State = {
    modalVisible: false,
  };

  componentDidMount() {
    this.props.actions.setExtraFilters([
      dsl.nestedTermFilter('taxonomies.taxons', get(this.props.object, 'attributes.name.v')),
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

  @autobind
  handleAddProduct(product: Product) {
    const { actions, params: { taxonId, context } } = this.props;

    actions.addProduct(taxonId, product.productId, context)
      .then(this.props.actions.fetch);
  }

  renderRow(row: Product, index: number, columns: Columns, params: Object) {
    return <ProductRow key={row.id} product={row} columns={columns} params={params} />;
  }

  render() {
    const { list, actions, addState } = this.props;
    const products = get(list, 'savedSearches[0].results.rows');

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
          isVisible={this.state.modalVisible}
          onCancel={this.closeModal}
          onConfirm={this.closeModal}
          onAddProduct={this.handleAddProduct}
          addState={addState}
          addedProducts={products}
        />
      </div>
    );
  }
}

const mapState = state => ({
  list: get(state, 'taxons.details.products'),
  addState: get(state.asyncActions, 'taxonAddProduct', {}),
});

const mapActions = dispatch => ({
  actions: {
    ...bindActionCreators(actions, dispatch),
    addProduct: bindActionCreators(addProduct, dispatch),
  },
});

export default connect(mapState, mapActions)(TaxonProductsPage);
