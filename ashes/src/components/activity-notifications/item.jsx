
// libs
import _ from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';

// components
import { DateTime } from '../common/datetime';
import AuthorTitle from '../activity-trail/activities/base/author-title';
import AuthorIcon from '../activity-trail/activities/base/author-icon';
import { representatives } from '../activity-trail/activities/index';
import { processActivity } from '../../modules/activity-trail';
import Icon from 'components/core/icon';

// cannot be stateless as it returns null
// related issue https://github.com/facebook/react/issues/4599
export default class NotificationItem extends React.Component {

  get typeIcon() {
    const type = _.get(this.props, ['item', 'kind']);
    if (!_.isEmpty(type)) {
      if (type.indexOf('order') >= 0) {
        return <Icon name="orders" />;
      } else if (type.indexOf('cart') >= 0) {
        return <Icon name="orders" />;
      } else if (type.indexOf('user') >= 0) {
        return <Icon name="customers" />;
      } else if (type.indexOf('customer') >= 0) {
        return <Icon name="customers" />;
      } else if (type.indexOf('gift_card') >= 0) {
        return <Icon name="gift-cards" />;
      } else if (type.indexOf('store_credit') >= 0) {
        return <Icon name="gift-cards" />;
      } else {
        return <Icon name="bell" />;
      }
    } else {
      return <Icon name="bell" />;
    }
  }

  render() {
    const isRead = _.get(this.props, ['item', 'isRead']);
    const classes = classNames('fc-activity-notification-item', {
      '_not-read': !isRead
    });
    const activity = processActivity(this.props.item);
    const desc = representatives[activity.kind];

    if (!desc) return null;

    const title = desc.title(activity.data, activity);

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
              <div className="fc-activity-notification-item__text">
                <AuthorTitle activity={activity} />&nbsp;{title}
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

}

NotificationItem.propTypes = {
  item: PropTypes.shape({
    createdAt: PropTypes.string,
    body: PropTypes.shape({
      origin: PropTypes.object.isRequired
    })
  })
};
