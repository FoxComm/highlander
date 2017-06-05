// @flow

// libs
import _ from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';

// components
import ContentBox from '../content-box/content-box';
import NotificationItem from '../activity-notifications/item';
import { PrimaryButton } from 'components/core/button';

// styles
import s from './panel.css';

// types
type Props = {
  notifications: Array<*>;
  displayed: boolean;
  markAsRead: Function;
  markAsReadAndClose: Function;
  className?: string;
};

export default class NotificationPanel extends React.Component {

  props: Props;

  get items() {
    const items = this.props.notifications;

    if (_.isEmpty(items)) {
      return (
        <div className={s.empty}>
          Nothing to see here yet!
        </div>
      );
    } else {
      return items.map(item => {
        return <NotificationItem item={item} key={`notification-item-${item.id}`} />;
      });
    }
  }

  get footer() {
    const items = this.props.notifications;
    const shouldBeDisabled = _.isEmpty(items);

    return (
      <div className={s.footer}>
        <PrimaryButton
          onClick={this.props.markAsReadAndClose}
          className={s.markAll}
          disabled={shouldBeDisabled}
        >
          Mark All As Read
        </PrimaryButton>
      </div>
    );
  }

  render() {
    if (this.props.displayed) {
      return (
        <ContentBox
          className={this.props.className}
          title="Notifications"
          footer={this.footer}
        >
          {this.items}
        </ContentBox>
      );
    }

    return null;
  }
}
