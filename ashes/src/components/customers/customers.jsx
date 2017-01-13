/**
 * @flow
 */

import React, { Component } from 'react';
import { actions } from '../../modules/customers/list';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';

import CustomerRow from './customer-row';
import { SelectableSearchList } from '../list-page';

const getState = state => ({ list: state.customers.list });

const mapDispatchToProps = dispatch => {
  return { actions: bindActionCreators(actions, dispatch) };
};

type Props = {
  list: Object,
  actions: Object,
  tableColumns?: Array<Object>,
};

class Customers extends Component {
  props: Props;

  static defaultProps = {
    tableColumns: [
      { field: 'name', text: 'Name' },
      { field: 'email', text: 'Email' },
      { field: 'id', text: 'Customer ID' },
      { field: 'shipRegion', text: 'Ship To Region' },
      { field: 'billRegion', text: 'Bill To Region' },
      { field: 'rank', text: 'Rank' },
      { field: 'joinedAt', text: 'Date/Time Joined', type: 'datetime' }
    ]
  };

  renderRow(row, index, columns, params) {
    const key = `customer-${row.id}`;

    return (
      <CustomerRow key={key}
                   customer={row}
                   columns={columns}
                   params={params} />
    );
  };

  render() {
    const {list, tableColumns, actions} = this.props;

    return (
      <SelectableSearchList
        entity="customers.list"
        emptyMessage="No customers found."
        list={list}
        renderRow={this.renderRow}
        tableColumns={tableColumns}
        searchActions={actions}
        searchOptions={{singleSearch: true}} />
    );
  }
}

export default connect(getState, mapDispatchToProps)(Customers);
