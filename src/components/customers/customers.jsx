'use strict';

import React from 'react';
import TableView from '../table/tableview';
import TableRow from '../table/row';
import TableCell from '../table/cell';
import TabListView from '../tabs/tabs';
import TabView from '../tabs/tab';
import DateTime from '../datetime/datetime';
import SearchBar from '../search-bar/search-bar';
import SectionTitle from '../section-title/section-title';
import { Link } from '../link';

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

  handleAddCustomerClick() {
    console.log('Add customer');
  }

  render() {
    let renderRow = (row, index) => {
      let params = {customer: row.id};
      return (
        <TableRow key={`customer-row-${row.id}`}>
          <TableCell><Link to='customer' params={params}>{ row.name }</Link></TableCell>
          <TableCell>{ row.email }</TableCell>
          <TableCell>{ row.id }</TableCell>
          <TableCell>{ row.shipRegion }</TableCell>
          <TableCell>{ row.billRegion }</TableCell>
          <TableCell>{ row.rank }</TableCell>
          <TableCell><DateTime value={ row.createdAt }/></TableCell>
        </TableRow>
      );
    };

    return (
      <div id="customers">
        <div className="fc-list-header">
          <SectionTitle title="Customers"
                        count={this.state.customers.length}
                        buttonClickHandler={ this.handleAddCustomerClick }/>
          <div className="fc-grid gutter">
            <div className="fc-col-md-1-1">
              <ul className="fc-tabbed-nav">
                <li><Link to="customers">Lists</Link></li>
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
          <div className="fc-col-md-1-1 fc-action-bar clearfix">
            <button className="fc-btn fc-right">
              <i className="icon-external-link"></i>
            </button>
          </div>
          <SearchBar />
          <TableView
            columns={this.props.tableColumns}
            rows={this.state.data.toArray()}
            model='customer'
            sort={CustomerStore.sort.bind(CustomerStore)} />
        </div>
      </div>
    );
  }
}
