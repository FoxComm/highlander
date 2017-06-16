
// libs
import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';

// components
import NotificationIndicator from './indicator';
import NotificationPanel from './panel';

// redux
import * as NotificationActions from '../../modules/activity-notifications';

@connect(state => state.activityNotifications, NotificationActions)
export default class NotificationBlock extends React.Component {

  static propTypes = {
    count: PropTypes.number,
    displayed: PropTypes.bool,
    notifications: PropTypes.array,
    markAsReadAndClose: PropTypes.func.isRequired,
    markAsRead: PropTypes.func.isRequired,
    toggleNotifications: PropTypes.func.isRequired,
    startFetchingNotifications: PropTypes.func.isRequired,
  };

  static defaultProps = {
    displayed: false
  };

  componentDidMount() {
    this.props.startFetchingNotifications();
  }

  render() {
    return (
      <div>
        <NotificationIndicator count={this.props.count}
                               displayed={this.props.displayed}
                               markAsReadAndClose={this.props.markAsReadAndClose}
                               toggleNotifications={this.props.toggleNotifications} />
        <NotificationPanel displayed={this.props.displayed}
                           notifications={this.props.notifications}
                           markAsRead={this.props.markAsRead}
                           markAsReadAndClose={this.props.markAsReadAndClose} />
      </div>
    );
  }

}
