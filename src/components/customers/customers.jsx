import React, { PropTypes } from 'react';
import TableView from '../table/tableview';
import TableRow from '../table/row';
import TableCell from '../table/cell';
import TabListView from '../tabs/tabs';
import TabView from '../tabs/tab';
import { DateTime } from '../common/datetime';
import SearchBar from '../search-bar/search-bar';
import SectionTitle from '../section-title/section-title';
import LocalNav from '../local-nav/local-nav';
import { Link } from '../link';
import { transitionTo } from '../../route-helpers';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import * as customersActions from '../../modules/customers/list';

@connect(state => ({customers: state.customers.customers}), customersActions)
export default class Customers extends React.Component {

  static defaultProps = {
    tableColumns: [
      {
        field: 'name',
        text: 'Name'
      },
      {
        field: 'email',
        text: 'Email'
      },
      {
        field: 'id',
        text: 'Customer ID'
      },
      {
        field: 'shipRegion',
        text: 'Ship To Region'
      },
      {
        field: 'billRegion',
        text: 'Bill To Region'
      },
      {
        field: 'rank',
        text: 'Rank'
      },
      {
        field: 'createdAt',
        text: 'Date/Time Joined',
        type: 'date'
      }
    ]
  }

  static contextTypes = {
    history: PropTypes.object.isRequired
  }

  static propTypes = {
    fetch: PropTypes.func,
    setFetchParams: PropTypes.func,
    customers: PropTypes.shape({
      total: PropTypes.number.isRequired
    }).isRequired,
    tableColumns: PropTypes.array
  }

  componentDidMount() {
    this.props.fetch(this.props.customers);
  }

  @autobind
  onAddCustomerClick() {
    transitionTo(this.context.history, 'customers-new');
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
      <div className="fc-list-page">
        <div className="fc-list-page-header">
          <SectionTitle title="Customers"
                        count={this.props.customers.total}
                        onAddClick={ this.onAddCustomerClick }
                        addTitle="Customer" />
          <LocalNav>
            <Link to="customers">Lists</Link>
            <a href="">Customer Groups</a>
            <a href="">Insights</a>
            <a href="">Activity Trial</a>
          </LocalNav>
          <TabListView>
            <TabView draggable={false}>All</TabView>
            <TabView>What</TabView>
          </TabListView>
        </div>
        <div className="fc-grid fc-list-page-content">
          <div className="fc-col-md-1-1 fc-action-bar fc-align-right">
            <button className="fc-btn">
              <i className="icon-external-link"></i>
            </button>
          </div>
          <SearchBar />
          <div className="fc-col-md-1-1">
            <TableView
              columns={this.props.tableColumns}
              data={this.props.customers}
              renderRow={renderRow}
              setState={this.props.setFetchParams}
              />
          </div>
        </div>
      </div>
    );
  }
}
