'use strict';

import React from 'react';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import ReturnsStore from './store';

export default class Returns extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      orders: ReturnsStore.getState()
    };
  }

  componentDidMount() {
    ReturnsStore.listenToEvent('change', this);
    ReturnsStore.fetch();
  }

  componentWillUnmount() {
    ReturnsStore.stopListeningToEvent('change', this);
  }

  onChangeReturnsStore() {
    this.setState({orders: ReturnsStore.getState()});
  }

  render() {
    return (
      <div id="returns">
        <div className="gutter">
          <table className="listing">
            <TableHead columns={this.props.tableColumns}/>
            <TableBody columns={this.props.tableColumns} rows={this.state.orders} model='order'>
              <input type="checkbox"/>
            </TableBody>
          </table>
        </div>
      </div>
    );
  }
}

Returns.propTypes = {
  tableColumns: React.PropTypes.array
};

Returns.defaultProps = {
  tableColumns: [
    {field: 'checked', text: ' ', component: 'checkbox'},
    {field: 'referenceNumber', text: 'Return', type: 'id'},
    {field: 'createdAt', text: 'Date', type: 'date'},
    {field: 'orderNumber', text: 'Order', type: 'id'},
    {field: 'email', text: 'Email'},
    {field: 'returnStatus', text: 'Return Status', type: 'returnStatus'},
    {field: 'returnTotal', text: 'Total', type: 'currency'}
  ]
};
