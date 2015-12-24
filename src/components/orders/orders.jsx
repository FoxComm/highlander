import React, { PropTypes } from 'react';
import TabListView from '../tabs/tabs';
import TabView from '../tabs/tab';
import TableView from '../table/tableview';
import SectionTitle from '../section-title/section-title';
import { connect } from 'react-redux';
import * as ordersActions from '../../modules/orders/list';
import LocalNav from '../local-nav/local-nav';
import { IndexLink } from '../link';
import LiveSearch from '../live-search/live-search';
import OrderRow from './order-row';
import util from 'util';
import _ from 'lodash';

@connect(state => ({
  list: state.orders.list
}), ordersActions.actions)
export default class Orders extends React.Component {
  static propTypes = {
    fetch: PropTypes.func.isRequired,
    tableColumns: PropTypes.array,
    subNav: PropTypes.array,
    orders: PropTypes.shape({
      rows: PropTypes.array.isRequired,
      total: PropTypes.number
    }),
    addSearchFilter: PropTypes.func,
    cloneSearch: PropTypes.func,
    deleteSearchFilter: PropTypes.func,
    editSearchNameStart: PropTypes.func,
    editSearchNameCancel: PropTypes.func,
    editSearchNameComplete: PropTypes.func,
    goBack: PropTypes.func,
    saveSearch: PropTypes.func,
    submitFilters: PropTypes.func,
    list: PropTypes.shape({
      selectedSearch: PropTypes.object,
      savedSearches: PropTypes.array
    }),
    selectSavedSearch: PropTypes.func
  };

  static defaultProps = {
    tableColumns: [
      {field: 'referenceNumber', text: 'Order ID', type: 'id', model: 'order'},
      {field: 'placedAt', text: 'Date/Time Placed', type: 'date'},
      {field: 'customer.name', text: 'Name'},
      {field: 'customer.email', text: 'Email'},
      {field: 'status', text: 'Order State', type: 'status', model: 'order'},
      {field: 'shipping.status', text: 'Shipment State', type: 'status', model: 'shipment'},
      {field: 'grandTotal', text: 'Total', type: 'currency'}
    ]
  };

  get selectedSearch() {
    return this.props.list.selectedSearch;
  }

  get orders() {
    return this.props.list.savedSearches[this.selectedSearch].results;
  }

  componentDidMount() {
    this.props.fetch('orders_search_view/_search');
  }

  handleAddOrderClick() {
    console.log('Add order clicked');
  }

  render() {
    const renderRow = (row, index) => <OrderRow order={row} columns={this.props.tableColumns} />;
    const filter = (searchTerm) => this.props.addSearchFilter('orders_search_view/_search', searchTerm);

    return (
      <div className="fc-list-page">
        <div className="fc-list-page-header">
          <SectionTitle title="Orders" subtitle={this.orders.total}
                        onAddClick={this.handleAddOrderClick }
                        addTitle="Order"
          />
          <LocalNav>
            <IndexLink to="orders">Lists</IndexLink>
            <a href="">Insights</a>
            <a href="">Activity Trail</a>
          </LocalNav>
        </div>
        <LiveSearch
          cloneSearch={this.props.cloneSearch}
          deleteSearchFilter={this.props.deleteSearchFilter}
          editSearchNameStart={this.props.editSearchNameStart}
          editSearchNameCancel={this.props.editSearchNameCancel}
          editSearchNameComplete={this.props.editSearchNameComplete}
          saveSearch={this.props.saveSearch}
          selectSavedSearch={this.props.selectSavedSearch}
          submitFilters={filter}
          searches={this.props.list}
        >
          <TableView
            columns={this.props.tableColumns}
            data={this.orders}
            renderRow={renderRow}
            setState={this.props.fetch}
          />
        </LiveSearch>
      </div>
    );
  }
}
