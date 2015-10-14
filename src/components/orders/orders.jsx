'use strict';

import React from 'react';
import TableView from '../tables/tableview';
import OrderStore from '../../stores/orders';
import OrderActions from '../../actions/orders';
import TabListView from '../tabs/tabs';
import TabView from '../tabs/tab';
import SectionTitle from '../section-title/section-title';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import * as actionCreators from '../../actions/orders';

function mapStateToProps(state) {
  return {
    orders: state.orders || {}
  };
}

function mapDispatchToProps(dispatch) {
  return { actions: bindActionCreators(actionCreators, dispatch) };
}

@connect(mapStateToProps, mapDispatchToProps)
export default class Orders extends React.Component {
  constructor(props, context) {
    super(props, context);
  }

  static propTypes = {
    tableColumns: React.PropTypes.array,
    subNav: React.PropTypes.array
  }

  static defaultProps = {
    tableColumns: [
      {field: 'referenceNumber', text: 'Order', type: 'id'},
      {field: 'createdAt', text: 'Date', type: 'date'},
      {field: 'email', text: 'Email'},
      {field: 'orderStatus', text: 'Order Status', type: 'orderStatus'},
      {field: 'paymentStatus', text: 'Payment Status'},
      {field: 'total', text: 'Total', type: 'currency'}
    ]
  }

  componentDidMount() {
    this.props.actions.fetchOrdersIfNeeded();
  }

  handleAddOrderClick() {
    console.log('Add order clicked');
  }

  render() {
    let orders = this.props.orders || [];
    
    return (
      <div id="orders">
        <div className="fc-list-header">
          <SectionTitle title="Orders" count={orders.size} buttonClickHandler={this.handleAddOrderClick }/>
          <div className="fc-grid gutter">
            <div className="fc-col-md-1-1">
              <ul className="fc-tabbed-nav">
                <li><a href="">Lists</a></li>
                <li><a href="">Returns</a></li>
              </ul>
            </div>
          </div>
          <TabListView>
            <TabView>What</TabView>
            <TabView>What</TabView>
          </TabListView>
        </div>
        <div className="gutter">
          <TableView
            columns={this.props.tableColumns}
            rows={orders.toArray()}
            model='order'
            sort={OrderStore.sort.bind(OrderStore)}
            />
        </div>
      </div>
    );
  }
}
