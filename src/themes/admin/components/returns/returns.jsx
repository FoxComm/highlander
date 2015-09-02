'use strict';

import React from 'react';
import Immutable from 'immutable';
import TableView from '../table/tableview';
import ReturnsStore from './store';

export default class Returns extends React.Component {
  constructor(props) {
    super(props);
    let immutableSet = Immutable.Set;
    this.state = {
      returns: ReturnsStore.getState(),
      selected: immutableSet()
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

  onSelectedChange(event) {
    let model = this.props.model;
    let checked = event.target.checked;
    this.setState({
      selected: this.state.selected[checked ? 'add' : 'delete'](model)
    });
  }

  getSelectedValue(model) {
    return this.state.selected.has(model);
  }

  render() {
    return (
      <div id="returns">
        <TableView
          columns={this.props.tableColumns}
          rows={this.state.returns}
          model='return'
        />
      </div>
    );
  }
}

Returns.propTypes = {
  tableColumns: React.PropTypes.array,
  model: React.PropTypes.object
};

Returns.defaultProps = {
  tableColumns: [
    {field: 'referenceNumber', text: 'Return', type: 'id'},
    {field: 'createdAt', text: 'Date', type: 'date'},
    {field: 'orderNumber', text: 'Order', model: 'order', type: 'id'},
    {field: 'email', text: 'Email'},
    {field: 'returnStatus', text: 'Return Status', type: 'returnStatus'},
    {field: 'returnTotal', text: 'Total', type: 'currency'}
  ]
};
