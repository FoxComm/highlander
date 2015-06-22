'use strict';

import React from 'react';
import TableHead from '../tables/head';
import TableBody from '../tables/body';
import NotificationStore from './store';
import ResendModal from './resend';
import { listenTo, stopListeningTo } from '../../lib/dispatcher';

const changeEvent = 'change-notification-store';

class Notifications extends React.Component {
  constructor(props) {
    super(props);
    this.onChangeNotificationStore = this.onChangeNotificationStore.bind(this);
    this.state = {
      notifications: NotificationStore.getState()
    };
  }

  componentDidMount() {
    listenTo(changeEvent, this);
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
          <TableBody columns={this.props.tableColumns} rows={this.state.notifications} model='notification'/>
        </table>
      </div>
    );
  }
}

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
    {field: 'id', text: 'Resend', type: 'dispatch', event: 'toggleModal', data: <ResendModal />}
  ]
};

export default Notifications;
