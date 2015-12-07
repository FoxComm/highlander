import React, { PropTypes } from 'react';
import { Button } from '../common/buttons';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import * as NotificationActions from '../../modules/activity-notifications';

@connect(state => ({notifications: state.activityNotifications}), NotificationActions)
export default class NotificationIndicator extends React.Component {

  static propTypes = {

  };

  componentDidMount() {
    this.props.fetchNotifications();
  }

  get indicator() {
    if (this.props.notifications.count > 0) {
      return (
        <div className="fc-activity-notifications__indicator">
          <span>{ this.props.notifications.count }</span>
        </div>
      );
    }
  }

  render() {
    return (
      <div className="fc-activity-notifications">
        <Button icon="bell" className="fc-activity-notifications__toggle">
          { this.indicator }
        </Button>
      </div>
    );
  }
}
