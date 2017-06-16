/* @flow */

// libs
import { get, flow } from 'lodash';
import { autobind, debounce } from 'core-decorators';
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import { createReducer, createAction } from 'redux-act';
import { makeLocalStore, addAsyncReducer } from '@foxcomm/wings';
import { createAsyncActions } from '@foxcomm/wings';

// components
import SearchInput from 'components/typeahead/input';
import LoadingInputWrapper from 'components/forms/loading-input-wrapper';
import { Table } from 'components/table';
import ProductRow from './product-row';

// redux
import { searchProducts } from 'elastic/products';

// styles
import styles from './products-add.css';

type Props = {
  search: string,
  setTerm: (term: string) => void,
  fetch: (token: string) => Promise<*>,
  fetchState: AsyncState,
  addState: AsyncState,
  products: Array<Product>,
  addedProductId: ?number,
  addedProducts: Array<Product>,
  setAddedProduct: (productId: ?number) => void,
  onAddProduct: (product: Product) => Promise<*>,
};

const tableColumns = [
  { field: 'productId', text: 'Product ID' },
  { field: 'image', text: 'Image', type: 'image' },
  { field: 'title', text: 'Name' },
  { field: 'skus', text: 'SKUs' },
  { field: 'add' },
];

class ProductsAdd extends Component {
  props: Props;

  @debounce(400)
  search() {
    this.props.fetch(this.props.search);
  }

  @autobind
  handleInputChange({ target }: { target: HTMLInputElement }) {
    this.props.setTerm(target.value);

    this.search();
  }

  // Workaround to handle single item fetch state
  @autobind
  handleAddProduct(product: Product) {
    this.props.setAddedProduct(product.productId);
    this.props.onAddProduct(product);
  }

  @autobind
  renderRow(row: Product) {
    const { addState, addedProducts, addedProductId } = this.props;

    const isNew = !addedProducts.some((p: Product) => p.productId === row.productId);
    const inProgress = addState.inProgress && addedProductId === row.productId;

    return (
      <ProductRow
        product={row}
        columns={tableColumns}
        params={{ onAdd: this.handleAddProduct, isNew, inProgress }}
        key={row.id}
      />
    );
  }

  render() {
    const { products, fetchState: { inProgress } } = this.props;

    return (
      <div className={styles.productSearch}>
        <LoadingInputWrapper inProgress={inProgress}>
          <SearchInput
            className={styles.search}
            value={this.props.search}
            onChange={this.handleInputChange}
            placeholder="Filter products..."
            autoFocus
          />
        </LoadingInputWrapper>

        <Table
          className={styles.table}
          emptyMessage="Search to find and add products"
          data={{ rows: products }}
          renderRow={this.renderRow}
          columns={tableColumns}
          isLoading={inProgress}
        />
      </div>
    );
  }
}

/*
 * Local redux store
 */
const fetch = createAsyncActions('fetchProducts', searchProducts);
const setTerm = createAction('setTerm');
const setAddedProduct = createAction('setAddedProduct');

const reducer = createReducer({
  [fetch.succeeded]: (state, response) => ({ ...state, products: get(response, 'result', []) }),
  [setAddedProduct]: (state, addedProductId) => ({ ...state, addedProductId }),
  [setTerm]: (state, term) => ({ ...state, term }),
});

const mapState = state => ({
  search: state.term,
  products: state.products,
  addedProductId: state.addedProductId,
  fetchState: get(state.asyncActions, 'fetchProducts', {}),
});

export default flow(
  connect(mapState, { fetch: fetch.perform, setTerm, setAddedProduct }),
  makeLocalStore(addAsyncReducer(reducer), { term: '', products: [] }),
)(ProductsAdd);
