import React, { PropTypes } from 'react';
import Link from '../link/link';
import { DateTime } from '../common/datetime';
import TabListView from '../tabs/tabs';
import TabView from '../tabs/tab';
import TableView from '../table/tableview';
import TableRow from '../table/row';
import TableCell from '../table/cell';
import SectionTitle from '../section-title/section-title';
import { connect } from 'react-redux';
import * as ordersActions from '../../modules/orders/list';
import * as searchActions from '../../modules/orders/search';
import LocalNav from '../local-nav/local-nav';
import Currency from '../common/currency';
import Status from '../common/status';
import LiveSearch from '../live-search/live-search';
import util from 'util';
import _ from 'lodash';

const actions = {
  ...ordersActions,
  ...searchActions
};

@connect(state => ({
  orders: state.orders.list,
  search: state.orders.search
}), actions)
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
      {field: 'name', text: 'Name'},
      {field: 'email', text: 'Email'},
      {field: 'orderStatus', text: 'Order State', type: 'status', model: 'order'},
      {field: 'paymentStatus', text: 'Payment State', type: 'status', model: 'payment'},
      {field: 'shippingStatus', text: 'Shipment State', type: 'status', model: 'shipment'},
      {field: 'total', text: 'Total', type: 'currency'}
    ]
  };

  componentDidMount() {
    this.props.fetch(this.props.orders);
  }

  handleAddOrderClick() {
    console.log('Add order clicked');
  }

  render() {
    const renderRow = (row, index) => (
      <TableRow key={`${index}`}>
        <TableCell>
          <Link to={'order'} params={{order: row.referenceNumber}}>
            {row.referenceNumber}
          </Link>
        </TableCell>
        <TableCell><DateTime value={row.placedAt}/></TableCell>
        <TableCell>{row.name}</TableCell>
        <TableCell>{row.email}</TableCell>
        <TableCell><Status value={row.orderStatus} model={"order"}/></TableCell>
        <TableCell><Status value={row.paymentStatus} model={"payment"}/></TableCell>
        <TableCell><Status value={row.shippingStatus} model={"shipment"}/></TableCell>
        <TableCell><Currency value={row.total}/></TableCell>
      </TableRow>
    );

    return (
      <div className="fc-list-page">
        <div className="fc-list-page-header">
          <SectionTitle title="Orders" subtitle={this.props.orders.total}
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
            data={this.props.orders}
            renderRow={renderRow}
            setState={this.props.fetch}
          />
        </LiveSearch>
      </div>
    );
  }
}
