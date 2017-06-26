// @flow

// libs
import _ from 'lodash';
import React from 'react';

// components
import ContentBox from '../content-box/content-box';
import NotificationItem from './item';
import { PrimaryButton } from 'components/core/button';

// styles
import s from './panel.css';

// types
type Props = {
  /** An array of objects, each object is a notification */
  notifications: Array<any>,
  /** If true, shows popup with a notifications list */
  displayed: boolean,
  /** A callback, which is supposed to mark all notifications as read outside the component */
  markAsRead: Function,
  /** A callback, which is supposed to mark all notifications as read + switch off `displayed` outside the component */
  markAsReadAndClose: Function,
  /** Custom css className for root html element of Panel component */
  className?: string,
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
    }

    return items.map(item => {
      return <NotificationItem item={item} key={`notification-item-${item.id}`} />;
    });
  }

  get footer() {
    return (
      <div className={s.footer}>
        <PrimaryButton
          onClick={this.props.markAsReadAndClose}
          className={s.markAll}
          disabled={_.isEmpty(this.props.notifications)}
        >
          Mark All As Read
        </PrimaryButton>
      </div>
    );
  }

  render() {
    if (!this.props.displayed) {
      return null;
    }

    return (
      <ContentBox className={this.props.className} title="Notifications" footer={this.footer}>
        {this.items}
      </ContentBox>
    );
  }
}
