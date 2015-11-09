'use strict';

import React, { PropTypes } from 'react';
import TableView from '../table/tableview';
import NotificationStore from '../../stores/notifications';
import ResendButton from './button';

export default class Notifications extends React.Component {
  static propTypes = {
    order: PropTypes.object,
    tableColumns: PropTypes.array,
    params: PropTypes.shape({
      order: PropTypes.string.isRequired
    }).isRequired
  };

  static defaultProps = {
    tableColumns: [
      {field: 'sendDate', text: 'Date', type: 'date'},
      {field: 'subject', text: 'Subject'},
      {field: 'contact', text: 'Contact Method'},
      {field: 'notificationStatus', text: 'Status'},
      {field: 'resendButton', text: 'Resend', component: 'ResendButton'}
    ]
  };

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
        <TableView columns={this.props.tableColumns} data={{rows: this.state.notifications}} />
      </div>
    );
  }
}
