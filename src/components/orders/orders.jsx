'use strict';

import React, { PropTypes } from 'react';
import TableView from '../table/tableview';
import TableRow from '../table/row';
import TableCell from '../table/cell';
import Link from '../link/link';
import { Date } from '../common/datetime';
import TabListView from '../tabs/tabs';
import TabView from '../tabs/tab';
import SectionTitle from '../section-title/section-title';
import { connect } from 'react-redux';
import * as ordersActions from '../../modules/orders/list';
import LocalNav from '../local-nav/local-nav';

@connect(state => ({orders: state.orders.list}), ordersActions)
export default class Orders extends React.Component {
  static propTypes = {
    fetch: PropTypes.func.isRequired,
    setFetchParams: PropTypes.func.isRequired,
    tableColumns: PropTypes.array,
    subNav: PropTypes.array,
    orders: PropTypes.shape({
      rows: PropTypes.array.isRequired,
      total: PropTypes.number
    })
  };

  static defaultProps = {
    tableColumns: [
      {field: 'referenceNumber', text: 'Order', type: 'id', model: 'order'},
      {field: 'createdAt', text: 'Date', type: 'date'},
      {field: 'email', text: 'Email'},
      {field: 'orderStatus', text: 'Order Status', type: 'orderStatus'},
      {field: 'paymentStatus', text: 'Payment Status'},
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
        <TableCell>{row.createdAt}</TableCell>
        <TableCell>{row.email}</TableCell>
        <TableCell>{row.orderStatus}</TableCell>
        <TableCell>{row.paymentStatus}</TableCell>
        <TableCell>{row.total}</TableCell>
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
            <a href="">Returns</a>
          </LocalNav>
          <TabListView>
            <TabView>What</TabView>
            <TabView>What</TabView>
          </TabListView>
        </div>
        <div className="fc-grid fc-list-page-content">
          <div className="fc-col-md-1-1">
            <TableView
              columns={this.props.tableColumns}
              data={this.props.orders}
              renderRow={renderRow}
              setState={this.props.setFetchParams}
            />
          </div>
        </div>
      </div>
    );
  }
}
