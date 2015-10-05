'use strict';

import React from 'react';
import TableView from '../tables/tableview';
import TabListView from '../tabs/tabs';
import TabView from '../tabs/tab';
import SearchBar from '../search-bar/search-bar';

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
    console.log(customers);
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
          <div className="fc-grid gutter">
            <div className="fc-col-1-1">
              <ul className="fc-tabbed-nav">
                <li><a href="">Lists</a></li>
                <li><a href="">Customer Groups</a></li>
                <li><a href="">Insights</a></li>
                <li><a href="">Activity Trial</a></li>
              </ul>
            </div>
          </div>
          <TabListView>
            <TabView draggable={false}>All</TabView>
            <TabView>What</TabView>
          </TabListView>
        </div>
        <div className="fc-grid gutter">
          <div className="fc-col-1-1 fc-action-bar clearfix">
            <button className="fc-btn fc-right">
              <i className="icon-external-link"></i>
            </button>
          </div>
          <SearchBar />
          <TableView
            columns={this.props.tableColumns}
            rows={this.state.customers}
            model='customer'
            sort={CustomerStore.sort.bind(CustomerStore)}
            limit={ 10 }
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
    {field: 'name', text: 'Name', type: 'link', id: 'id'},
    {field: 'email', text: 'Email', type: 'link', id: 'id'},
    {field: 'id', text: 'Customer ID', type: 'id'},
    {field: 'shipToRegion', text: 'Ship To Region'},
    {field: 'billToRegion', text: 'Bill To Region'},
    {field: 'rank', text: 'Rank'},
    {field: 'createdAt', text: 'Date/Time Joined', type: 'date'}
  ]
};
