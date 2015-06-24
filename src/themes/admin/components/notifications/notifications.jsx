'use strict';

import React from 'react';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import NotificationStore from './store';
import ResendButton from './button';
import { listenTo, stopListeningTo } from '../../lib/dispatcher';

const changeEvent = 'change-notification-store';

export default class Notifications extends React.Component {
  constructor(props) {
    super(props);
    this.onChangeNotificationStore = this.onChangeNotificationStore.bind(this);
    this.state = {
      notifications: NotificationStore.getState()
    };
  }

  componentDidMount() {
    listenTo(changeEvent, this);

    let { router } = this.context,
      order = router.getCurrentParams().order;

    NotificationStore.uriRoot = `/orders/${order}`;
    NotificationStore.fetch();
  }

  componentWillUnmount() {
    stopListeningTo(changeEvent, this);
  }

  onChangeNotificationStore() {
    this.setState({orders: NotificationStore.getState()});
  }

  render() {
    return (
      <div id="notifications">
        <table className='listing'>
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
  order: React.PropTypes.number,
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
