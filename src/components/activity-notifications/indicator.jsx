import React, { PropTypes } from 'react';
import classNames from 'classNames';
import { Button } from '../common/buttons';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import * as NotificationActions from '../../modules/activity-notifications';

@connect(state => ({notifications: state.activityNotifications}), NotificationActions)
export default class NotificationIndicator extends React.Component {

  static propTypes = {
    notifications: PropTypes.shape({
      count: PropTypes.number.isRequired,
      displayed: PropTypes.bool.isRequired
    }),
    toggleNotifications: PropTypes.func,
    fetchNotifications: PropTypes.func
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
    const classes = classNames('fc-activity-notifications__toggle', {
      '_active': this.props.notifications.displayed
    });
    return (
      <div className="fc-activity-notifications">
        <Button icon="bell"
                className={ classes }
                onClick={ this.props.toggleNotifications }>
          { this.indicator }
        </Button>
      </div>
    );
  }
}
