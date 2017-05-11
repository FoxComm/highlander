/* @flow */

import React, { Element, Component } from 'react';

// libs
import _ from 'lodash';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// components
import { SelectableSearchList } from '../list-page';
import CartRow from './cart-row';

// actions
import { actions } from 'modules/carts/list';
import { bulkExport } from 'modules/bulk-export/bulk-export';

const tableColumns = [
  {field: 'referenceNumber', text: 'Cart'},
  {field: 'createdAt', text: 'Date/Time Placed', type: 'datetime'},
  {field: 'customer.name', text: 'Customer Name'},
  {field: 'customer.email', text: 'Customer Email'},
  {field: 'grandTotal', text: 'Total', type: 'currency'}
];

type Props = {
  list: Object,
  actions: Object,
  bulkExportAction: (fields: Array<string>, entity: string, identifier: string) => Promise<*>,

}

class Carts extends Component {
  props: Props;

  get renderRow() {
    return (row, index, columns, params) => {
      const key = `cart-${row.referenceNumber}`;

      return (
        <CartRow
          cart={row}
          columns={columns}
          key={key}
          params={params}
        />
      );
    };
  }

  render() {
    const {list, actions} = this.props;

    return (
      <SelectableSearchList
        exportEntity="carts"
        bulkExport
        bulkExportAction={this.props.bulkExportAction}
        entity="carts.list"
        emptyMessage="No carts found."
        list={list}
        renderRow={this.renderRow}
        tableColumns={tableColumns}
        searchActions={actions}
        predicate={cart => cart.referenceNumber}
      />
    );
  }
}

const mapStateToProps = (state) => {
  return {
    list: _.get(state.carts, 'list', {}),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkExportAction: bindActionCreators(bulkExport, dispatch),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(Carts);
