
// libs
import _ from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';

// components
import ContentBox from '../content-box/content-box';
import NotificationItem from '../activity-notifications/item';
import { PrimaryButton } from 'components/core/button';

export default class NotificationPanel extends React.Component {

  static propTypes = {
    notifications: PropTypes.array.isRequired,
    displayed: PropTypes.bool,
    markAsRead: PropTypes.func.isRequired,
    markAsReadAndClose: PropTypes.func.isRequired
  };

  static defaultProps = {
    displayed: false
  }

  get items() {
    const items = this.props.notifications;
    if (_.isEmpty(items)) {
      return (
        <div className="fc-activity-notifications__empty-message">
          Nothing to see here yet!
        </div>
      );
    } else {
      return items.map(item => {
        return (<NotificationItem item={item} key={`notification-item-${item.id}`} />);
      });
    }
  }

  get footer() {
    const items = this.props.notifications;
    const shouldBeDisabled = _.isEmpty(items);
    const buttonClassName = classNames('fc-activity-notifications__footer-button', {
      '_disabled': shouldBeDisabled
    });
    return (
      <div className="fc-activity-notifications__footer">
        <PrimaryButton onClick={this.props.markAsReadAndClose}
                       className={buttonClassName}
                       disabled={shouldBeDisabled}>
          Mark All As Read
        </PrimaryButton>
      </div>
    );
  }

  get body() {
    if (this.props.displayed) {
      return (
        <div>
          <div className="fc-activity-notifications__overlay"
               onClick={this.props.markAsRead}>
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
