'use strict';

import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import * as rmaActions from '../../modules/rmas/list';
import { renderRow } from './helpers';
import TableView from '../table/tableview';
import { get } from 'sprout-data';


@connect(state => ({rmas: state.rmas.list}), rmaActions)
export default class RmaChildList extends React.Component {
  static propTypes = {
    tableColumns: PropTypes.array,
    fetchRmas: PropTypes.func.isRequired,
    setFetchParams: PropTypes.func.isRequired,
    entity: PropTypes.object,
    rmas: PropTypes.shape({
      order: PropTypes.object.isRequired,
      customer: PropTypes.object.isRequired
    }),
    paginationState: PropTypes.func
  };

  static defaultProps = {
    tableColumns: [
      {field: 'referenceNumber', text: 'Return', type: 'id'},
      {field: 'createdAt', text: 'Date', type: 'date'},
      {field: 'orderId', text: 'Order', model: 'order', type: 'id'},
      {field: 'email', text: 'Email', component: 'RmaEmail'},
      {field: 'status', text: 'Return Status', type: 'rmaStatus'},
      {field: 'returnTotal', text: 'Total', component: 'RmaTotal'}
    ]
  };

  componentDidMount() {
    this.props.fetchRmas(this.entity);
  }

  get entity() {
    return this.props.entity;
  }

  get entityType() {
    return this.entity.entityType;
  }

  get data() {
    return get(this.props.rmas, [this.entityType, this.entity.entityId], rmaActions.paginationState);
  }

  @autobind
  setFetchParams(state, fetchParams) {
    this.props.setFetchParams(state, this.entity, fetchParams);
  }

  render() {
    return (
      <TableView
          data={this.data}
          columns={this.props.tableColumns}
          setState={this.setFetchParams}
          renderRow={renderRow}
      />
    );
  }
}
