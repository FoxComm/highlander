import React, { PropTypes } from 'react';
import TableView from '../table/tableview';
import TableRow from '../table/row';
import TableCell from '../table/cell';
import TabListView from '../tabs/tabs';
import TabView from '../tabs/tab';
import { DateTime } from '../common/datetime';
import SearchBar from '../search-bar/search-bar';
import CustomersBase from './base';
import { IndexLink, Link } from '../link';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import * as customersActions from '../../modules/customers/list';

import SectionTitle from '../section-title/section-title';
import MultiSelectTable from '../table/multi-select-table';
import LocalNav from '../local-nav/local-nav';
import LiveSearch from '../live-search/live-search';
import CustomerRow from './customer-row';

@connect(state => ({
  customers: state.customers.customers
}), customersActions.actions)
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
      // {
      //   field: 'rank',
      //   text: 'Rank'
      // },
      // {
      //   field: 'createdAt',
      //   text: 'Date/Time Joined',
      //   type: 'date'
      // }
    ]
  };

  static propTypes = {
    fetch: PropTypes.func.isRequired,
    customers: PropTypes.object,
    tableColumns: PropTypes.array,
    addSearchFilter: PropTypes.func,
    cloneSearch: PropTypes.func,
    editSearchNameStart: PropTypes.func,
    editSearchNameCancel: PropTypes.func,
    editSearchNameComplete: PropTypes.func,
    saveSearch: PropTypes.func,
    selectSearch: PropTypes.func
  };

  get selectedSearch() {
    return this.props.customers.selectedSearch;
  }

  get customers() {
    return this.props.customers.savedSearches[this.selectedSearch].results;
  }

  render() {
    const url = 'customers_search_view/_search';
    const filter = (searchTerm) => this.props.addSearchFilter(url, searchTerm);
    const selectSearch = (idx) => this.props.selectSearch(url, idx);
    const renderRow = (row, index, columns) => {
      const key = `customer-${row.id}`;
      return <CustomerRow customer={row} columns={columns} key={key} />;
    };
          // <div className="fc-col-md-1-1 fc-action-bar fc-align-right">
          //   <button className="fc-btn">
          //     <i className="icon-external-link"/>
          //   </button>
          // </div>

    return (
      <div className="fc-list-page">
        <div className="fc-list-page-header">
          <SectionTitle title="Customers"
                      subtitle={ this.props.customers.total }
                      onAddClick={ this.onAddCustomerClick }
                      addTitle="Customer" />
          <LocalNav>
            <IndexLink to="customers">Lists</IndexLink>
            <IndexLink to="groups">Customer Groups</IndexLink>
            <a href="">Insights</a>
            <a href="">Activity Trial</a>
          </LocalNav>
        </div>
        <LiveSearch
          cloneSearch={this.props.cloneSearch}
          editSearchNameStart={this.props.editSearchNameStart}
          editSearchNameCancel={this.props.editSearchNameCancel}
          editSearchNameComplete={this.props.editSearchNameComplete}
          saveSearch={this.props.saveSearch}
          selectSavedSearch={selectSearch}
          submitFilters={filter}
          searches={this.props.customers}
        >
          <MultiSelectTable
            columns={this.props.tableColumns}
            data={this.customers}
            renderRow={renderRow}
            setState={this.props.fetch}
            emptyMessage="No customers found." />
        </LiveSearch>
      </div>
    );
  }
}
