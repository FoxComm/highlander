/* @flow */

// libs
import get from 'lodash/get';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// actions
import { actions } from 'modules/taxons/details/products-list';
import { addProduct, deleteProduct as unlinkProduct } from 'modules/taxons/details/taxon';

// components
import { SectionTitle } from 'components/section-title';
import SelectableSearchList from 'components/list-page/selectable-search-list';
import ProductRow from 'components/products/product-row';
import { makeTotalCounter } from 'components/list-page';
import { ProductsAddModal } from 'components/products-add';
import { Button } from 'components/common/buttons';

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
  deleteState: AsyncState,
  params: TaxonomyParams & {
    taxonId: number,
  },
};

type State = {
  modalVisible: boolean,
  deletedProductId: ?number,
}

export class TaxonProductsPage extends Component {
  props: Props;

  state: State = {
    modalVisible: false,
    deletedProductId: null,
  };

  componentDidMount() {
    this.props.actions.setExtraFilters([
      dsl.nestedTermFilter('taxonomies.taxons', get(this.props.object, 'attributes.name.v')),
    ]);

    this.props.actions.fetch();
  }

  get tableColumns(): Columns {
    return [
      { field: 'productId', text: 'ID' },
      { field: 'image', text: 'Image', type: 'image' },
      { field: 'title', text: 'Name' },
      { field: 'skus', text: 'SKUs' },
      { field: 'state', text: 'State' },
      { field: '', render: this.unlinkButton },
    ];
  }

  @autobind
  unlinkButton(children: any, row: Product) {
    const inProgress = this.props.deleteState.inProgress && this.state.deletedProductId === row.productId;

    return (
      <Button
        onClick={this.handleUnlinkProduct.bind(this, row)}
        isLoading={inProgress}
      >
        Unlink
      </Button>
    );
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

    actions.addProduct(product.productId, context, taxonId)
      .then(this.props.actions.fetch);
  }

  @autobind
  handleUnlinkProduct(product: Product, e: MouseEvent) {
    e.preventDefault();
    e.stopPropagation();

    const { actions, params: { taxonId, context } } = this.props;

    this.setState({ deletedProductId: product.productId }, () => {
      actions.unlinkProduct(product.productId, context, taxonId)
        .then(this.props.actions.fetch);

      return;
    });
  }

  renderRow(row: Product, index: number, columns: Columns, params: Object) {
    return (
      <ProductRow
        key={row.id}
        product={row}
        columns={columns}
        params={params}
      />
    );
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
          tableClassName={styles.productsTable}
          list={list}
          renderRow={this.renderRow}
          tableColumns={this.tableColumns}
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
  deleteState: get(state.asyncActions, 'taxonDeleteProduct', {}),
});

const mapActions = dispatch => ({
  actions: {
    ...bindActionCreators(actions, dispatch),
    addProduct: bindActionCreators(addProduct, dispatch),
    unlinkProduct: bindActionCreators(unlinkProduct, dispatch),
  },
});

export default connect(mapState, mapActions)(TaxonProductsPage);
