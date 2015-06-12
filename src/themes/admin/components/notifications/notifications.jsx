'use strict';

import React from 'react';
import { RouteHandler } from 'react-router';
import TableHead from '../tables/head';
import TableBody from '../tables/body';

class Notifications extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      tableRows: this.generateNotifications()
    };
  }

  generateNotifications() {
    let
      idx           = 10,
      notifications = [],
      status        = ['Delivered', 'Failed'],
      subject       = ['Shipment confirmation', 'Order confirmation', 'Review your items'];

    while (idx--) {
      notifications.push({
        notification: 1000 + idx,
        date: new Date().toISOString(),
        status: status[~~(Math.random() * status.length)],
        subject: subject[~~(Math.random() * subject.length)]
      });
    }

    return notifications;
  }

  render() {
    return (
      <div id="notifications">
        <table className='listing'>
          <TableHead columns={this.props.tableColumns}/>
          <TableBody columns={this.props.tableColumns} rows={this.state.tableRows} model='notification'/>
        </table>
        <RouteHandler/>
      </div>
    );
  }
}

Notifications.contextTypes = {
  router: React.PropTypes.func
};

Notifications.propTypes = {
  order: React.PropTypes.number,
  tableColumns: React.PropTypes.array
};

Notifications.defaultProps = {
  tableColumns: [
    {field: 'notification', text: 'ID'},
    {field: 'date', text: 'Date', type: 'date'},
    {field: 'subject', text: 'Subject'},
    {field: 'status', text: 'Status'}
  ]
};

export default Notifications;
