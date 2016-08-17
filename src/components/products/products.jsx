/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import _ from 'lodash';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import * as dsl from 'elastic/dsl';

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
  { field: 'productId', text: 'Product ID', type: null },
  { field: 'image', text: 'Image', type: 'image' },
  { field: 'title', text: 'Name', type: null },
  { field: 'state', text: 'State', type: null },
];

export class Products extends Component<void, Props, void> {
  props: Props;

  componentDidMount() {
    this.props.actions.setExtraFilters([
      dsl.existsFilter('archivedAt', 'missing')
    ]);

    this.props.actions.fetch();
  }

  //it worked the same way with componentWillReceiveProps
  componentDidUpdate(prevProps) {
    const { list } = this.props;

    const prevSearchId = prevProps.list.selectedSearch;
    const currentSearchId = list.selectedSearch;
    const prevSearchQuery = prevProps.list.savedSearches[prevSearchId].query;
    const currentSearchQuery = list.savedSearches[currentSearchId].query;

    //if I don't check this, set extra filters will loop forever
    if (currentSearchId === prevSearchId && _.isEqual(prevSearchQuery.sort(), currentSearchQuery.sort())) return;

    //in the ideal world if you search with archivedAt term, archived items should be shown
    if (_.find(currentSearchQuery, 'term', 'archivedAt')) {
      this.props.actions.clearExtraFilters();
      //this.props.actions.submitFilters(currentSearchQuery);
      this.props.actions.fetch();
    } else {
      this.props.actions.setExtraFilters([
        dsl.existsFilter('archivedAt', 'missing')
      ]);

      //this.props.actions.submitFilters(currentSearchQuery);
      this.props.actions.fetch();
    }
  }

  renderRow(row: Product, index: number, columns: Array<Column>, params: Object) {
    const key = `products-${row.id}`;
    return <ProductRow key={key} product={row} columns={columns} params={params} />;
  }

  render(): Element {
    const { list, actions } = this.props;

    return (
      <div className="fc-products-list">
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
