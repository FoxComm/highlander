'use strict';

import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import SectionTitle from '../section-title/section-title';
import LocalNav from '../local-nav/local-nav';
import { TabListView, TabView } from '../tabs';
import { Link } from '../link';
import { connect } from 'react-redux';
import * as rmaActions from '../../modules/rmas/list';
import TableView from '../table/tableview';
import { renderRow } from './helpers';
import { assoc } from 'sprout-data';

function mapStateToProps(state) {
  const rmaData = state.rmas.list;

  return {
    'rmas': assoc(
      rmaData,
      'rows', rmaData.rows || []
    )
  };
}

@connect(mapStateToProps, rmaActions)
export default class Rmas extends React.Component {
  static propTypes = {
    tableColumns: PropTypes.array,
    fetchRmas: PropTypes.func.isRequired,
    rmas: PropTypes.shape({
      total: PropTypes.number
    })
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
          <SectionTitle title="Returns" subtitle={this.props.rmas.total} />
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
                data={this.props.rmas}
                columns={this.props.tableColumns}
                setState={(data, params) => this.props.fetchRmas(params)}
                renderRow={renderRow}
            />
          </div>
        </div>
      </div>
    );
  }
}
