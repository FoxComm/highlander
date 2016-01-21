import React, { PropTypes } from 'react';
import { actions } from '../../modules/customers/list';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';

import CustomerRow from './customer-row';
import ListPage from '../list-page/list-page';

const getState = state => ({ list: state.customers.list });

const mapDispatchToProps = dispatch => {
  return { actions: bindActionCreators(actions, dispatch) };
};

const Customers = props => {
  const navLinks = [
    { title: 'Lists', to: 'customers' },
    { title: 'Customer Groups', to: 'groups' },
    { title: 'Insights', to: '' },
    { title: 'Activity Trail', to: '' }
  ];

  const renderRow = (row, index, columns) => {
    const key = `customer-${row.id}`;
    return <CustomerRow customer={row} columns={columns} key={key} />;
  };

  const tableColumns = [
    { field: 'name', text: 'Name' },
    { field: 'email', text: 'Email' },
    { field: 'id', text: 'Customer ID' },
    { field: 'shipRegion', text: 'Ship To Region' },
    { field: 'billRegion', text: 'Bill To Region' },
    { field: 'rank', text: 'Rank' },
    { field: 'joinedAt', text: 'Date/Time Joined', type: 'datetime' }
  ];

  return (
    <ListPage
      addTitle="Customer"
      emptyResultMessage="No customers found."
      list={props.list}
      navLinks={navLinks}
      renderRow={renderRow}
      tableColumns={tableColumns}
      searchActions={props.actions}
      title="Customers"
      url="customers_search_view/_search" />        
  );
};

export default connect(getState, mapDispatchToProps)(Customers);
