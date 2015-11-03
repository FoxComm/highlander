'use strict';

import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import * as rmaActions from '../../modules/rmas/list';
import { RmaList } from './helpers';


@connect(({rmas}) => ({list: rmas.list}), rmaActions)
export default class RmaChildList extends React.Component {
  static propTypes = {
    tableColumns: PropTypes.array,
    fetchChildRmas: PropTypes.func.isRequired,
    entity: PropTypes.object,
    list: PropTypes.shape({
      orderRmas: PropTypes.object.isRequired,
      customerRmas: PropTypes.object.isRequired
    })
  };

  static defaultProps = {
    tableColumns: [
      {field: 'referenceNumber', text: 'Return', type: 'id'},
      {field: 'createdAt', text: 'Date', type: 'date'},
      {field: 'orderId', text: 'Order', model: 'order', type: 'id'},
      {field: 'email', text: 'Email', component: 'StoreAdminEmail'},
      {field: 'status', text: 'Return Status', type: 'rmaStatus'},
      {field: 'returnTotal', text: 'Total', component: 'RmaTotal'}
    ]
  };

  componentDidMount() {
    this.props.fetchChildRmas(this.entity);
  }

  get entity() {
    return this.props.entity;
  }

  get entityType() {
    return this.entity.entityType;
  }

  get items() {
    const list = this.props.list;
    let thing;
    if (this.entityType === 'order') {
      thing = list.orderRmas[this.entity.referenceNumber];
    } else if (this.entityType === 'customer') {
      thing = list.customerRmas[this.entity.id];
    }

    if (thing && !thing.isFetching) {
      return thing.items;
    } else {
      return [];
    }
  }

  render() {
    return <RmaList items={this.items} tableColumns={this.props.tableColumns} />;
  }
}
