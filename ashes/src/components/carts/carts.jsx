// @flow
import React, { Element, Component } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

// data
import { actions } from 'modules/carts/list';

// components
import { SelectableSearchList } from '../list-page';
import CartRow from './cart-row';

const mapStateToProps = (state) => {
  return {
    list: state.carts.list,
  };
};

const mapDispatchToProps = dispatch => {
  return {
    actions: bindActionCreators(actions, dispatch),
  };
};

const tableColumns = [
  {field: 'referenceNumber', text: 'Cart'},
  {field: 'createdAt', text: 'Date/Time Placed', type: 'datetime'},
  {field: 'customer.name', text: 'Customer Name'},
  {field: 'customer.email', text: 'Customer Email'},
  {field: 'grandTotal', text: 'Total', type: 'currency'}
];

type Props = {
  list: Object,
  actions: Object
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

export default connect(mapStateToProps, mapDispatchToProps)(Carts);
