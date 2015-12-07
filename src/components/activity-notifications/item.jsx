import React, { PropTypes } from 'react';
import { DateTime } from '../common/datetime';

const NotificationItem = (props) => {
  return (
    <div className="fc-activity-notification-item">
      <div className="fc-activity-notification-item__content">
        <div className="fc-activity-notification-item__time">
          <DateTime value={props.item.createdAt} />
        </div>
        <div className="fc-activity-notification-item__info">
          <div className="fc-activity-notification-item__type">
          </div>
          <div className="fc-activity-notification-item__body">
            <div className="fc-activity-notification-item__author">
            </div>
            <div className="fc-activity-notification-item__text">
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default NotificationItem;
