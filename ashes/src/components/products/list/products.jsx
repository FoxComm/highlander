// @flow

// libs
import React, { Component, Element } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { filterArchived } from 'elastic/archive';
import styles from './products.css';

// data
import { actions } from 'modules/products/list';

// components
import SelectableSearchList from 'components/list-page/selectable-search-list';
import ProductRow from './product-with-variants-row';

// types
import type { Product } from 'paragons/product';
import type { SearchFilter } from 'elastic/common';

type Column = {
  field: string,
  text: string,
  type?: string,
};

type Props = {
  actions: Object,
  list: Object,
};

const tableColumns: Array<Column> = [
  { field: 'image', text: 'Image', type: 'image' },
  { field: 'title', text: 'Name' },
  { field: 'variants', text: 'Variants' },
  { field: 'skuCode', text: 'SKU'},
  { field: 'createdAt', text: 'Date/Time created', type: 'datetime'},
  { field: 'retailPrice', text: 'Price', type: 'currency', currencyField: 'retailPriceCurrency'},
  { field: 'state', text: 'State' },
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

  render(): Element {
    const { list, actions } = this.props;

    const searchActions = {
      ...actions,
      addSearchFilters: this.addSearchFilters,
    };

    return (
      <div className="fc-products-list">
        <SelectableSearchList
          tableClass={styles['products-table']}
          entity="products.list"
          emptyMessage="No products found."
          list={list}
          renderRow={this.renderRow}
          wrapToTbody={false}
          tableColumns={tableColumns}
          searchActions={searchActions}
        />
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
