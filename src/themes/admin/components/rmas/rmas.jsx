'use strict';

import React from 'react';
import Immutable from 'immutable';
import TableView from '../tables/tableview';
import RmaStore from './store';

export default class Rmas extends React.Component {
  constructor(props) {
    super(props);
    let immutableSet = Immutable.Set;
    this.state = {
      rmas: RmaStore.getState(),
      selected: immutableSet()
    };
  }

  componentDidMount() {
    RmaStore.listenToEvent('change', this);
    RmaStore.fetch();
  }

  componentWillUnmount() {
    RmaStore.stopListeningToEvent('change', this);
  }

  onChangeRmaStore(rmas) {
    this.setState({rmas});
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
      <div id="rmas">
        <div className="gutter">
          <TableView
            columns={this.props.tableColumns}
            rows={this.state.rmas}
            model='rma'
            sort={RmaStore.sort.bind(RmaStore)}
            />
        </div>
      </div>
    );
  }
}

Rmas.propTypes = {
  tableColumns: React.PropTypes.array,
  model: React.PropTypes.object
};

Rmas.defaultProps = {
  tableColumns: [
    {field: 'referenceNumber', text: 'Return', type: 'id'},
    {field: 'createdAt', text: 'Date', type: 'date'},
    {field: 'orderNumber', text: 'Order', model: 'order', type: 'id'},
    {field: 'email', text: 'Email'},
    {field: 'returnStatus', text: 'Return Status', type: 'returnStatus'},
    {field: 'returnTotal', text: 'Total', type: 'currency'}
  ]
};
