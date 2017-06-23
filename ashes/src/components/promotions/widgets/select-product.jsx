/* @flow */

import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { searchProducts } from '../../../elastic/products';

import { SearchDropdown } from 'components/core/dropdown';

// styles
import s from './select-product.css';

import type { Context } from '../types';

type RefId = string | number;

type ProductSearch = {
  productSearchId: RefId,
};

type Props = {
  context: Context,
  name: string,
};

type State = {
  products: Array<any>,
};

export default class SelectProduct extends Component {
  props: Props;
  state: State = {
    products: [],
  };

  get search(): Array<ProductSearch> {
    return _.get(this.props.context.params, this.props.name, []);
  }

  updateSearches(searches: Array<ProductSearch>): void {
    this.props.context.setParams({
      [this.props.name]: searches,
    });
  }

  get selectedProduct(): ?RefId {
    return _.get(this.search, '0.productSearchId');
  }

  @autobind
  handleProductSearch(token: string): Promise<*> {
    return searchProducts(token, {
      omitArchived: true,
      omitInactive: true,
    }).then(result => {
      const items = result.result.map(({ id, title }) => [id, title]);

      this.setState({ products: result.result });

      return { items, token };
    });
  }

  @autobind
  renderProductOption(value: string) {
    const product = this.state.products.find(item => item.id == value);

    if (!product) {
      return null;
    }

    return (
      <div>
        {product.title}
        <span className={s.description}> • ID: {product.id}</span>
      </div>
    );
  }

  @autobind
  handleSelectProduct(value: string) {
    console.log('value', value);
    this.updateSearches([
      {
        productSearchId: value,
      },
    ]);
  }

  render() {
    console.log(this.props);
    return (
      <SearchDropdown
        name="selectProduct"
        placeholder="- Select Product -"
        searchbarPlaceholder="Product name or ID"
        value={this.selectedProduct}
        fetch={this.handleProductSearch}
        renderItem={this.renderProductOption}
        onChange={this.handleSelectProduct}
      />
    );
  }
}
