'use strict';

import React from 'react';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import NotificationStore from './store';
import ResendButton from './button';

export default class Notifications extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = {
      notifications: NotificationStore.getState()
    };
  }

  componentDidMount() {
    NotificationStore.listenToEvent('change', this);

    // @TODO: this code was not prepared for rma
    const { order } = this.props.params;

    NotificationStore.uriRoot = `/orders/${order}`;
    NotificationStore.fetch();
  }

  componentWillUnmount() {
    NotificationStore.stopListeningToEvent('change', this);
  }

  onChangeNotificationStore(orders) {
    this.setState({orders: orders});
  }

  render() {
    return (
      <div id="notifications">
        <table className="fc-table">
          <TableHead columns={this.props.tableColumns}/>
          <TableBody columns={this.props.tableColumns} rows={this.state.notifications} model='notification'>
            <ResendButton />
          </TableBody>
        </table>
      </div>
    );
  }
}

Notifications.contextTypes = {
  router: React.PropTypes.func
};

Notifications.propTypes = {
  order: React.PropTypes.object,
  tableColumns: React.PropTypes.array
};

Notifications.defaultProps = {
  tableColumns: [
    {field: 'sendDate', text: 'Date', type: 'date'},
    {field: 'subject', text: 'Subject'},
    {field: 'contact', text: 'Contact Method'},
    {field: 'notificationStatus', text: 'Status'},
    {field: 'resendButton', text: 'Resend', component: 'ResendButton'}
  ]
};
