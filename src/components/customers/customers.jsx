'use strict';

import React from 'react';
import Panel from '../panel/panel';
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

export default class Customers extends React.Component {
  static propTypes = {
    tableColumns: React.PropTypes.array
  };

  static defaultProps = {
    tableColumns: [
      {field: 'name', text: 'Name', type: 'link', id: 'id'},
      {field: 'email', text: 'Email', type: 'link', id: 'id'},
      {field: 'id', text: 'Customer ID', type: 'id'},
      {field: 'shipRegion', text: 'Ship To Region'},
      {field: 'billRegion', text: 'Bill To Region'},
      {field: 'rank', text: 'Rank'},
      {field: 'createdAt', text: 'Date/Time Joined', type: 'date'}
    ]
  };

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

  handleAddCustomerClick() {
    console.log("Add customer");
  }

  render() {
    let renderRow = (row, index) => {
      let params = {customer: row};
      return (
        <div>
          <TableRow>
            <TableCell><Link to='customer' params={params}>{ row.name }</Link></TableCell>
            <TableCell>{ row.email }</TableCell>
            <TableCell>{ row.id }</TableCell>
            <TableCell>{ row.shipRegion }</TableCell>
            <TableCell>{ row.billRegion }</TableCell>
            <TableCell>{ row.rank }</TableCell>
            <TableCell><DateTime value={ row.createdAt }/></TableCell>
          </TableRow>
        </div>
      );
    }

    return (
      <div id="users">
        <div className="fc-list-header">
          <SectionTitle title="Cutomers" count={this.state.customers.length} buttonClickHandler={ this.handleAddCustomerClick }/>
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
          <TableView store={CustomerStore} renderRow={renderRow.bind(this)} empty={'No customers yet.'}/>
        </div>
      </div>
    );
  }
}
