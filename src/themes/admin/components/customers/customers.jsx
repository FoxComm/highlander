'use strict';

import React from 'react';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import CustomerStore from './store';

export default class Customers extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      customers: CustomerStore.getState()
    };
  }

  componentDidMount() {
    CustomerStore.listenToEvent('change', this);
    CustomerStore.fetch();
  }

  componentWillUnmount() {
    CustomerStore.stopListeningToEvent('change', this);
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
    {field: 'disabled', text: 'Disabled', type: 'bool'},
    {field: 'createdAt', text: 'Date Joined', type: 'date'}
  ]
};
