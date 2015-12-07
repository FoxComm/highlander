import React, { PropTypes } from 'react';
import ContentBox from '../content-box/content-box';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import * as NotificationActions from '../../modules/activity-notifications';

@connect(state => ({notifications: state.activityNotifications}), NotificationActions)
export default class NotificationPanel extends React.Component {

  get body() {
    if (this.props.notifications.displayed) {
      return (
        <ContentBox title='Notifications'>
          Notifications!
        </ContentBox>
      );
    }
  }

  render() {
    return (
      <div className="fc-activity-notifications__panel">
        { this.body }
      </div>
    );
  }

}
