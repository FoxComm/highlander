
/* @flow */

import _ from 'lodash';
import React, { Element, Component } from 'react';
import { autobind } from 'core-decorators';
import { searchProducts } from '../../../elastic/products';

import DropdownSearch from '../../dropdown/dropdown-search';
import DropdownItem from '../../dropdown/dropdownItem';

import styles from './select-product.css';

import type { Context } from '../types';

type RefId = string|number;

type ProductSearch = {
  productSearchId: RefId;
};

type Props = {
  context: Context;
  name: string;
}

export default class SelectProduct extends Component {
  props: Props;

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

  handleProductSearch(token: string): Promise<*> {
    return searchProducts(token).then((result) => {
      return result.result;
    });
  }

  @autobind
  renderProductOption(product: Object) {
    return (
      <DropdownItem value={product.id} key={`${product.id}`}>
        <span>{ product.title }</span>
        <span styleName="product-description">• ID: { product.id }</span>
      </DropdownItem>
    );
  }

  @autobind
  handleSelectProduct(value: string) {
    this.updateSearches([{
      productSearchId: value,
    }]);
  }

  render() {
    return (
      <DropdownSearch
        className="select-product-dd"
        name="selectProduct"
        placeholder="- Select Product -"
        styleName="full-width"
        searchbarPlaceholder="Product name or ID"
        value={this.selectedProduct}
        fetchOptions={this.handleProductSearch}
        renderOption={this.renderProductOption}
        onChange={this.handleSelectProduct}
      />
    );
  }
}
