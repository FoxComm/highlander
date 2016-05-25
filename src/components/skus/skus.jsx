/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// data
import { actions } from '../../modules/skus/list';

// components
import SelectableSearchList from '../list-page/selectable-search-list';
import SkuRow from './sku-row';

type Column = {
  field: string,
  text: string,
  type: ?string,
};

type Props = {
  actions: Object,
  list: Object,
};

type Sku = {
  id: number;
  code: string,
  title: string,
  price: string,
};

export class Skus extends Component<void, Props, void> {
  props: Props;

  static tableColumns: Array<Column> = [
    { field: 'code', text: 'Code', type: null },
    { field: 'title', text: 'title', type: null },
    { field: 'price', text: 'Price', type: 'currency' }
  ];

  renderRow(row: Sku, index: number, columns: Array<Column>, params: Object) {
    const key = `skus-${row.id}`;
    return <SkuRow key={key} sku={row} columns={columns} params={params} />;
  }

  render(): Element {
    const { list, actions } = this.props;

    return (
      <div>
        <SelectableSearchList
          emptyMessage="No SKUs found."
          list={list}
          renderRow={this.renderRow}
          tableColumns={Skus.tableColumns}
          searchActions={actions}
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
