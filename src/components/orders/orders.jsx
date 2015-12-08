import React, { PropTypes } from 'react';
import TableView from '../table/tableview';
import TableRow from '../table/row';
import TableCell from '../table/cell';
import Link from '../link/link';
import { DateTime } from '../common/datetime';
import TabListView from '../tabs/tabs';
import TabView from '../tabs/tab';
import SectionTitle from '../section-title/section-title';
import { connect } from 'react-redux';
import * as ordersActions from '../../modules/orders/list';
import LocalNav from '../local-nav/local-nav';
import Currency from '../common/currency';
import Status from '../common/status';
import LiveSearch from '../live-search/live-search';

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
    }),
    deleteSearchFilter: PropTypes.func,
    goBack: PropTypes.func,
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
          <TabListView>
            <TabView draggable={false} selected={true}>All</TabView>
            <TabView>Remorse Hold</TabView>
            <TabView>Last 30 Days</TabView>
            <TabView>Manual Hold</TabView>
            <TabView>Fraud Hold</TabView>
          </TabListView>
        </div>
        <div className="fc-grid fc-list-page-content">
          <LiveSearch 
            submitFilter={this.props.submitFilter}
            state={this.props.orders}
            goBack={this.props.goBack}
            deleteSearchFilter={this.props.deleteSearchFilter}
          />
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
