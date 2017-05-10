import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import * as rmaActions from '../../modules/rmas/list';
import { renderRow } from './helpers';
import TableView from '../table/tableview';

@connect(state => ({rmas: state.rmas.list}), rmaActions)
export default class RmaChildList extends React.Component {
  static propTypes = {
    tableColumns: PropTypes.array,
    updateStateAndFetch: PropTypes.func,
    fetchRmas: PropTypes.func.isRequired,
    entity: PropTypes.object.isRequired,
    rmas: PropTypes.shape({
      rows: PropTypes.array.isRequired,
      total: PropTypes.number
    }).isRequired,
  };

  static defaultProps = {
    tableColumns: [
      {field: 'referenceNumber', text: 'Return', type: 'id'},
      {field: 'createdAt', text: 'Date', type: 'date'},
      {field: 'orderId', text: 'Order', model: 'order', type: 'id'},
      {field: 'email', text: 'Email'},
      {field: 'state', text: 'Return State', type: 'rmaStatus'},
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
          setState={this.props.updateStateAndFetch}
          renderRow={renderRow}
      />
    );
  }
}
