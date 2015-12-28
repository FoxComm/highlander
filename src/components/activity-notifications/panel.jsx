
// libs
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// components
import ContentBox from '../content-box/content-box';
import NotificationItem from '../activity-notifications/item';
import { PrimaryButton } from '../common/buttons';

export default class NotificationPanel extends React.Component {

  static propTypes = {
    notifications: PropTypes.array.isRequired,
    displayed: PropTypes.bool,
    toggleNotifications: PropTypes.func.isRequired,
    markAsRead: PropTypes.func.isRequired,
    markAsReadAndClose: PropTypes.func.isRequired
  };

  static defaultProps = {
    displayed: false
  }

  get items() {
    const items = this.props.notifications;
    return items.map(item => {
      return (<NotificationItem item={item} key={`notification-item-${item.id}`}/>);
    });
  }

  get footer() {
    return (
      <div className="fc-activity-notifications__footer">
        <PrimaryButton onClick={this.props.markAsRead}
                       className="fc-activity-notifications__footer-button">
          View All
        </PrimaryButton>
      </div>
    );
  }

  get body() {
    if (this.props.displayed) {
      return (
        <div>
          <div className="fc-activity-notifications__overlay"
               onClick={this.props.markAsReadAndClose}>
          </div>
          <ContentBox title="Notifications"
                      className="fc-activity-notifications__box"
                      footer={this.footer}>
            {this.items }
          </ContentBox>
        </div>
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
