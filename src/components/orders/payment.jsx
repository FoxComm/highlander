'use strict';

import _ from 'lodash';
import React, { PropTypes } from 'react';
import PaymentMethod from './payment-method';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import Panel from '../panel/panel';

export default class OrderPayment extends React.Component {
  static propTypes = {
    order: PropTypes.object,
    tableColumns: PropTypes.array
  }

  static defaultProps = {
    tableColumns: [
      {field: 'paymentMethod', text: 'Method', component: 'PaymentMethod'},
      {field: 'amount', text: 'Amount', type: 'currency'},
      {field: 'status', text: 'Status'},
      {field: 'createdAt', text: 'Date/Time', type: 'date'}
    ]
  }

  constructor(props, context) {
    super(props, context);
    this.state = {
      methods: [],
      isEditing: false
    };
  }

  toggleEdit() {
    this.setState({
      isEditing: !this.state.isEditing
    });
  }

  render() {
    let order = this.props.order;

    let editButton = null;

    if (!this.state.isEditing) {
      editButton = (
        <div>
          <button className="fc-btn icon-edit fc-right" onClick={this.toggleEdit.bind(this)}></button>
        </div>
      );
    }

    return (
      <Panel className="fc-order-payment"
             title="Payment"
             controls={ editButton }>
        <table className="fc-table">
          <TableHead columns={this.props.tableColumns}/>
          <TableBody columns={this.props.tableColumns} rows={_.compact([order.payment])} model='payment-method'>
            <PaymentMethod/>
          </TableBody>
        </table>
      </Panel>
    );
  }
}
