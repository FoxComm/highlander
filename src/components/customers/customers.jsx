'use strict';

import React from 'react';
import TableView from '../tables/tableview';
import CustomerStore from '../../stores/customers';

export default class Customers extends React.Component {
  constructor(props, context) {
    super(props, context);
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

  onChangeCustomerStore(customers) {
    this.setState({customers});
  }

  render() {
    return (
      <div id="users">
        <div className="fc-list-header">
          <div className="fc-grid gutter">
            <div className="fc-col-2-6">
              <h1 className="fc-title">Cutomers <span className="fc-subtitle">{this.state.customers.length}</span></h1>
            </div>
            <div className="fc-col-2-6 fc-push-2-6 fc-actions">
              <button className="fc-btn fc-btn-primary"><i className="icon-add"></i> Customer</button>
            </div>
          </div>
        </div>
        <div className="gutter">
          <TableView
            columns={this.props.tableColumns}
            rows={this.state.customers}
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
