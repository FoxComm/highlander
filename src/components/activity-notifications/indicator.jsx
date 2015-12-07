import React, { PropTypes } from 'react';
import { Button } from '../common/buttons';

export default class NotificationIndicator extends React.Component {

  static propTypes = {
    notificationsCount: PropTypes.number
  };

  get indicator() {
    if (this.props.notificationsCount > 0) {
      return (
        <div className="fc-activity-notifications__indicator">
          <span>{ this.props.notificationsCount }</span>
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
