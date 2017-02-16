/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// data
import { actions } from 'modules/products/list';

// components
import SelectableSearchList from '../list-page/selectable-search-list';
import ProductRow from './product-row';

// helpers
import { filterArchived } from 'elastic/archive';

// types
import type { Product } from 'paragons/product';
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

const tableColumns: Array<Column> = [
  { field: 'productId', text: 'Product ID', type: null },
  { field: 'image', text: 'Image', type: 'image' },
  { field: 'title', text: 'Name', type: null },
  { field: 'state', text: 'State', type: null },
];

export class Products extends Component {
  props: Props;

  @autobind
  addSearchFilters(filters: Array<SearchFilter>, initial: boolean = false) {
    return this.props.actions.addSearchFilters(filterArchived(filters), initial);
  }

  renderRow(row: Product, index: number, columns: Array<Column>, params: Object) {
    const key = `products-${row.id}`;
    return <ProductRow key={key} product={row} columns={columns} params={params} />;
  }

  render() {
    const { list, actions } = this.props;

    const searchActions = {
      ...actions,
      addSearchFilters: this.addSearchFilters,
    };

    return (
      <div className="fc-products-list">
        <SelectableSearchList
          entity="products.list"
          emptyMessage="No products found."
          list={list}
          renderRow={this.renderRow}
          tableColumns={tableColumns}
          searchActions={searchActions}
          predicate={({id}) => id} />
      </div>
    );
  }
}

function mapStateToProps({ products: { list } }) {
  return { list };
}

function mapDispatchToProps(dispatch) {
  return {
    actions: bindActionCreators(actions, dispatch),
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(Products);
