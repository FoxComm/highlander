import React, { PropTypes } from 'react';
import TabListView from '../tabs/tabs';
import TabView from '../tabs/tab';
import TableView from '../table/tableview';
import SectionTitle from '../section-title/section-title';
import { connect } from 'react-redux';
import * as searchActions from '../../modules/orders/search';
import LocalNav from '../local-nav/local-nav';
import LiveSearch from '../live-search/live-search';
import OrderRow from './order-row';
import util from 'util';
import _ from 'lodash';

@connect(state => ({
  search: state.orders.search
}), searchActions.actions)
export default class Orders extends React.Component {
  static propTypes = {
    fetch: PropTypes.func.isRequired,
    tableColumns: PropTypes.array,
    subNav: PropTypes.array,
    orders: PropTypes.shape({
      rows: PropTypes.array.isRequired,
      total: PropTypes.number
    }),
    cloneSearch: PropTypes.func,
    deleteSearchFilter: PropTypes.func,
    editSearchNameStart: PropTypes.func,
    editSearchNameCancel: PropTypes.func,
    editSearchNameComplete: PropTypes.func,
    goBack: PropTypes.func,
    saveSearch: PropTypes.func,
    submitFilter: PropTypes.func
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
    return this.props.search.selectedSearch;
  }

  get orders() {
    return this.props.search.savedSearches[this.selectedSearch].results;
  }

  componentDidMount() {
    this.props.fetch('orders_search_view/_search');
  }

  handleAddOrderClick() {
    console.log('Add order clicked');
  }

  render() {
    const renderRow = (row, index) => <OrderRow order={row} columns={this.props.tableColumns} />;

    return (
      <div className="fc-list-page">
        <div className="fc-list-page-header">
          <SectionTitle title="Orders" subtitle={this.orders.total}
                        onAddClick={this.handleAddOrderClick }
                        addTitle="Order"
          />
          <LocalNav>
            <a href="">Lists</a>
            <a href="">Insights</a>
            <a href="">Activity Trail</a>
          </LocalNav>
        </div>
        <LiveSearch
          cloneSearch={this.props.cloneSearch}
          goBack={this.props.goBack}
          deleteSearchFilter={this.props.deleteSearchFilter}
          editSearchNameStart={this.props.editSearchNameStart}
          editSearchNameCancel={this.props.editSearchNameCancel}
          editSearchNameComplete={this.props.editSearchNameComplete}
          saveSearch={this.props.saveSearch}
          selectSavedSearch={this.props.selectSavedSearch}
          submitFilter={this.props.submitFilter}
          searches={this.props.search}
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
