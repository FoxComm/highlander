/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// data
import { actions } from '../../modules/products/list';

// components
import { SearchableList } from '../list-page';
import ProductRow from './product-row';

type Column = {
  field: string,
  text: string,
  type: ?string,
};

type Props = {
  actions: Object,
  list: Object,
};

type Product = {
  id: number,
  image: string,
  title: string,
};

export class Products extends Component<void, Props, void> {
  static tableColumns: Array<Column> = [
    { field: 'id', text: 'Product ID', type: null },
    { field: 'image', text: 'Image', type: 'image' },
    { field: 'title', text: 'Name', type: null },
  ];

  renderRow(row: Product, index: number, columns: Array<Column>, params: Object) {
    const key = `products-${row.id}`;
    return <ProductRow key={key} product={row} columns={columns} params={params} />;
  }

  render(): Element {
    const { list, actions } = this.props;

    return (
      <div>
        <SearchableList
          emptyResultMessage="No products found."
          list={list}
          renderRow={this.renderRow}
          tableColumns={Products.tableColumns}
          searchActions={actions}
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
