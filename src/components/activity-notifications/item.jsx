
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';

// components
import { DateTime } from '../common/datetime';
import UserInitials from '../users/initials';
import { IndexLink, Link } from '../link';
import AuthorTitle from '../activity-trail/activities/base/author-title';
import AuthorIcon from '../activity-trail/activities/base/author-icon';
import { representatives } from '../activity-trail/activities/index';
import { processActivity } from '../../modules/activity-trail';

// cannot be stateless as it returns null
// related issue https://github.com/facebook/react/issues/4599
export default class NotificationItem extends React.Component {

  get typeIcon() {
    const type = _.get(this.props, ['item', 'kind']);
    if (!_.isEmpty(type)) {
      if (type.indexOf('order') >= 0) {
        return <i className="icon icon-orders"></i>;
      } else if (type.indexOf('cart') >= 0) {
        return <i className="icon icon-orders"></i>;
      } else if (type.indexOf('customer') >= 0) {
        return <i className="icon icon-customers"></i>;
      } else if (type.indexOf('gift_card') >= 0) {
        return <i className="icon icon-gift-cards"></i>;
      } else if (type.indexOf('store_credit') >= 0) {
        return <i className="icon icon-gift-cards"></i>;
      } else {
        return <i className="icon icon-bell"></i>;
      }
    } else {
      return <i className="icon icon-bell"></i>;
    }
  }

  render() {
    const origin = _.get(this.props, ['item', 'data', 'admin']);
    const isRead = _.get(this.props, ['item', 'isRead']);
    const classes = classNames('fc-activity-notification-item', {
      '_not-read': !isRead
    });
    const activity = processActivity(this.props.item);
    const desc = representatives[activity.kind];

    if (!desc) return null;

    const args = [activity.data, activity];

    const title = desc.title(...args);

    return (
      <div className={ classes }>
        <div className="fc-activity-notification-item__content">
          <div className="fc-activity-notification-item__time">
            <DateTime value={this.props.item.createdAt} />
          </div>
          <div className="fc-activity-notification-item__info">
            <div className="fc-activity-notification-item__type">
              {this.typeIcon}
            </div>
            <div className="fc-activity-notification-item__body">
              <div className="fc-activity-notification-item__author">
                <AuthorIcon activity={activity} />
              </div>
              <div className="fc-activity__description">
                <AuthorTitle activity={activity} />&nbsp;{title}
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

};

NotificationItem.propTypes = {
  item: PropTypes.shape({
    createdAt: PropTypes.string,
    body: PropTypes.shape({
      origin: PropTypes.object.isRequired
    })
  })
};

export default NotificationItem;
