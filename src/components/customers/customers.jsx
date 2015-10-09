'use strict';

import React from 'react';
import TableView from '../tables/tableview';
import CustomerStore from '../../stores/customers';
import CustomerActions from '../../actions/customers';

export default class Customers extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = {
      data: CustomerStore.getState()
    };
    this.onChange = this.onChange.bind(this);
  }

  componentDidMount() {
    CustomerStore.listen(this.onChange);

    CustomerActions.fetchCustomers();
  }

  componentWillUnmount() {
    CustomerStore.unlisten(this.onChange);
  }

  onChange() {
    this.setState({
      data: CustomerStore.getState()
    });
  }

  render() {
    return (
      <div id="users">
        <div className="gutter">
          <TableView
            columns={this.props.tableColumns}
            rows={this.state.data.toArray()}
            model='customer'
            sort={CustomerStore.sort.bind(CustomerStore)}
            />
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
