/* @flow */

// libs
import React, { Component, Element } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// actions
import { actions } from 'modules/skus/list';

// components
import SelectableSearchList from '../list-page/selectable-search-list';
import SkuRow from './sku-row';

// helpers
import { filterArchived } from 'elastic/archive';

type Props = {
  actions: Object,
  list: Object,
};

export class Skus extends Component {
  props: Props;

  static tableColumns: Columns = [
    { field: 'skuCode', text: 'SKU' },
    { field: 'title', text: 'Title' },
    { field: 'salePrice', currencyField: 'salePriceCurrency', text: 'Sale Price', type: 'currency' },
    { field: 'retailPrice', currencyField: 'retailPriceCurrency', text: 'Retail Price', type: 'currency' }
  ];

  @autobind
  addSearchFilters(filters: Array<SearchFilter>, initial: boolean = false) {
    return this.props.actions.addSearchFilters(filterArchived(filters), initial);
  }

  renderRow(row: SkuSearchItem, index: number, columns: Columns, params: Object) {
    const key = `skus-${row.id}`;
    return (
      <SkuRow
        key={key}
        sku={row}
        columns={columns}
        params={params}
      />
    );
  }

  render() {
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
          predicate={({skuCode}) => skuCode}
        />
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    list: _.get(state.skus, 'list', {}),
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    actions: bindActionCreators(actions, dispatch),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(Skus);
