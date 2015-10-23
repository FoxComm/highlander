'use strict';

import React from 'react';
import TableView from '../tables/tableview';
import SectionTitle from '../section-title/section-title';
import LocalNav from '../local-nav/local-nav';
import { TabListView, TabView } from '../tabs';
import { Link } from '../link';
import { connect } from 'react-redux';
import * as rmaActions from '../../modules/rmas/list';

@connect(({rmas}) => ({items: rmas.list.items}), rmaActions)
export default class Rmas extends React.Component {
  static propTypes = {
    tableColumns: React.PropTypes.array,
    model: React.PropTypes.object
  };

  static defaultProps = {
    tableColumns: [
      {field: 'referenceNumber', text: 'Return', type: 'id'},
      {field: 'createdAt', text: 'Date', type: 'date'},
      {field: 'orderNumber', text: 'Order', model: 'order', type: 'id'},
      {field: 'email', text: 'Email'},
      {field: 'returnStatus', text: 'Return Status', type: 'returnStatus'},
      {field: 'returnTotal', text: 'Total', type: 'currency'}
    ]
  };

  componentDidMount() {
    this.props.fetchRmasIfNeeded();
  }

  render() {
    return (
      <div className="fc-list-page">
        <div className="fc-list-page-header">
          <SectionTitle title="Returns" subtitle={this.props.items.length} />
          <LocalNav>
            <a href="">Lists</a>
            <a href="">Returns</a>
          </LocalNav>
          <TabListView>
            <TabView>All</TabView>
            <TabView>Active</TabView>
          </TabListView>
        </div>
        <div className="fc-grid fc-list-page-content">
          <div className="fc-col-md-1-1">
            <TableView
              columns={this.props.tableColumns}
              rows={this.props.items}
              model='rma'
            />
          </div>
        </div>
      </div>
    );
  }
}
