/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// data
import { actions } from 'modules/skus/list';

// components
import SelectableSearchList from '../list-page/selectable-search-list';
import SkuRow from './sku-row';

// helpers
import { filterArchived } from 'elastic/archive';

// types
import type { Sku } from 'modules/skus/list';
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

export class Skus extends Component {
  props: Props;

  static tableColumns: Array<Column> = [
    { field: 'skuCode', text: 'SKU', type: null },
    { field: 'title', text: 'Title', type: null },
    { field: 'salePrice', currencyField: 'salePriceCurrency', text: 'Sale Price', type: 'currency' },
    { field: 'retailPrice', currencyField: 'retailPriceCurrency', text: 'Retail Price', type: 'currency' }
  ];

  @autobind
  addSearchFilters(filters: Array<SearchFilter>, initial: boolean = false) {
    return this.props.actions.addSearchFilters(filterArchived(filters), initial);
  }

  renderRow(row: Sku, index: number, columns: Array<Column>, params: Object) {
    const key = `skus-${row.id}`;
    return <SkuRow key={key} sku={row} columns={columns} params={params} />;
  }

  render(): Element {
    const { list, actions } = this.props;

    const searchActions = {
      ...actions,
      addSearchFilters: this.addSearchFilters,
    };

    return (
      <div>
        <SelectableSearchList
          entity="skus.list"
          emptyMessage="No SKUs found."
          list={list}
          renderRow={this.renderRow}
          tableColumns={Skus.tableColumns}
          searchActions={searchActions}
          predicate={({code}) => code} />
      </div>
    );
  }
}

function mapStateToProps({ skus: { list } }) {
  return { list };
}

function mapDispatchToProps(dispatch) {
  return {
    actions: bindActionCreators(actions, dispatch),
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(Skus);
