/* @flow */

// libs
import get from 'lodash/get';
import { autobind, debounce } from 'core-decorators';
import React, { Component, Element } from 'react';

// components
import { ModalContainer } from 'components/modal/base';
import SearchInput from 'components/typeahead/input';
import LoadingInputWrapper from 'components/forms/loading-input-wrapper';
import { Table } from 'components/table';
import ProductRow from './product-row';

// actions
import { searchProducts } from 'elastic/products';

// styles
import styles from './product-add.css';

type Props = {
  addState: AsyncState,
  addedProducts: Array<Product>,
  onAddProduct: (productId: number) => Promise<*>,
};

type State = {
  search: string,
  products: Array<Product>,
  inProgress: boolean,
};

const tableColumns = [
  { field: 'productId', text: 'Product ID' },
  { field: 'image', text: 'Image', type: 'image' },
  { field: 'title', text: 'Name' },
  { field: 'skus', text: 'SKUs' },
  { field: 'add' },
];

class ProductsSearch extends Component {
  props: Props;

  state: State = {
    search: '',
    products: [],
    inProgress: false,
  };

  @debounce(400)
  search() {
    this.setState({ inProgress: true }, () =>
      searchProducts(this.state.search)
        .then(response => this.setState({
          products: get(response, 'result', []),
          inProgress: false
        }))
        .catch({ inProgress: false })
    );
  }

  @autobind
  handleInputChange({ target }: InputEvent) {
    this.setState({ search: target.value });

    this.search();
  }

  @autobind
  renderRow(row: Product) {
    const { addState, addedProducts, onAddProduct } = this.props;
    return (
      <ProductRow
        product={row}
        columns={tableColumns}
        params={{ onAdd: onAddProduct, addState, addedProducts }}
        key={row.productId}
      />
    );
  }

  render() {
    return (
      <div className={styles.productSearch}>
        <LoadingInputWrapper inProgress={this.state.inProgress}>
          <SearchInput
            className={styles.search}
            value={this.state.search}
            onChange={this.handleInputChange}
            focusOnMount
          />
        </LoadingInputWrapper>

        <Table
          className={styles.table}
          emptyMessage="Search to find and add products"
          data={{ rows: this.state.products }}
          renderRow={this.renderRow}
          columns={tableColumns}
          isLoading={this.state.inProgress}
        />
      </div>
    );
  }
}

export default ProductsSearch;
