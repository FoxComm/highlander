import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import * as rmaActions from '../../modules/rmas/list';
import { renderRow } from './helpers';
import TableView from '../table/tableview';
import { get } from 'sprout-data';

function mapStateToProps(state, {entity}) {
  const rmaData = get(state.rmas.list, [entity.entityType, entity.entityId], {rows: []});

  return {
    'rmas': rmaData
  };
}


@connect(mapStateToProps, rmaActions)
export default class RmaChildList extends React.Component {
  static propTypes = {
    tableColumns: PropTypes.array,
    fetchRmas: PropTypes.func.isRequired,
    entity: PropTypes.object,
    rmas: PropTypes.object.isRequired
  };

  static defaultProps = {
    tableColumns: [
      {field: 'referenceNumber', text: 'Return', type: 'id'},
      {field: 'createdAt', text: 'Date', type: 'date'},
      {field: 'orderId', text: 'Order', model: 'order', type: 'id'},
      {field: 'email', text: 'Email'},
      {field: 'status', text: 'Return Status', type: 'rmaStatus'},
      {field: 'returnTotal', text: 'Total', type: 'currency'}
    ]
  };

  componentDidMount() {
    this.props.fetchRmas(this.props.entity);
  }

  render() {
    return (
      <TableView
          data={this.props.rmas}
          columns={this.props.tableColumns}
          setState={(data, params) => this.props.fetchRmas(params)}
          renderRow={renderRow}
      />
    );
  }
}
