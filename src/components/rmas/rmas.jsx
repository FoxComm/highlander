'use strict';

import React, { PropTypes } from 'react';
import SectionTitle from '../section-title/section-title';
import LocalNav from '../local-nav/local-nav';
import { TabListView, TabView } from '../tabs';
import { Link } from '../link';
import { connect } from 'react-redux';
import * as rmaActions from '../../modules/rmas/list';
import {RmaList} from './helpers';

@connect(({rmas}) => ({items: rmas.list.items}), rmaActions)
export default class Rmas extends React.Component {
  static propTypes = {
    tableColumns: PropTypes.array,
    fetchRmas: PropTypes.func.isRequired,
    items: PropTypes.array.isRequired
  };

  static defaultProps = {
    tableColumns: [
      {field: 'referenceNumber', text: 'Return', type: 'id'},
      {field: 'createdAt', text: 'Date', type: 'date'},
      {field: 'orderRefNum', text: 'Order', model: 'order', type: 'id'},
      {field: 'email', text: 'Email', component: 'RmaEmail'},
      {field: 'status', text: 'Return Status', type: 'rmaStatus'},
      {field: 'returnTotal', text: 'Total', component: 'RmaTotal'}
    ]
  };

  componentDidMount() {
    this.props.fetchRmas();
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
            <RmaList {...this.props} />
          </div>
        </div>
      </div>
    );
  }
}
