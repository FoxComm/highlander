'use strict';

import React from 'react';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import CustomerStore from './store';
import { listenTo, stopListeningTo } from '../../lib/dispatcher';

const changeEvent = 'change-customer-store';

export default class Customers extends React.Component {
  constructor(props) {
    super(props);
    this.onChangeCustomerStore = this.onChangeCustomerStore.bind(this);
    this.state = {
      customers: CustomerStore.getState()
    };
  }

  componentDidMount() {
    listenTo(changeEvent, this);
    CustomerStore.fetch();
  }

  componentWillUnmount() {
    stopListeningTo(changeEvent, this);
  }

  onChangeCustomerStore() {
    this.setState({customers: CustomerStore.getState()});
  }

  render() {
    return (
      <div id="users">
        <div className="gutter">
          <table className='listing'>
            <TableHead columns={this.props.tableColumns}/>
            <TableBody columns={this.props.tableColumns} rows={this.state.customers}/>
          </table>
        </div>
      </div>
    );
  }
}

Customers.propTypes = {
  tableColumns: React.PropTypes.array
};

Customers.defaultProps = {
  tableColumns: [
    {field: 'firstName', text: 'First Name'},
    {field: 'lastName', text: 'Last Name'},
    {field: 'email', text: 'Email'},
    {field: 'role', text: 'Role'},
    {field: 'blocked', text: 'Blocked'},
    {field: 'cause', text: 'Cause'},
    {field: 'dateJoined', text: 'Date Joined', type: 'date'}
  ]
};
