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
import SelectableSearchList from '../list-page/selectable-search-list';
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

const tableColumns: Array<Column> = [
  { field: 'id', text: 'Product ID', type: null },
  { field: 'image', text: 'Image', type: 'image' },
  { field: 'title', text: 'Name', type: null },
];

export class Products extends Component<void, Props, void> {

  renderRow(row: Product, index: number, columns: Array<Column>, params: Object) {
    const key = `products-${row.id}`;
    return <ProductRow key={key} product={row} columns={columns} params={params} />;
  }

  render(): Element {
    const { list, actions } = this.props;

    return (
      <div>
        <SelectableSearchList
          emptyMessage="No products found."
          list={list}
          renderRow={this.renderRow}
          tableColumns={tableColumns}
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
