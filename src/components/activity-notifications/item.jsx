import _ from 'lodash';
import React, { PropTypes } from 'react';
import { DateTime } from '../common/datetime';

const icon = (item) => {
  console.log(item);
  const type = _.get(item, ['body', 'reference', 'typed']);
  console.log(type);
  switch(type) {
    case 'Order':
      return (<i className='icon icon-orders'></i>);
    default:
      return (<i className='icon icon-bell'></i>);
  }
};

const NotificationItem = (props) => {
  return (
    <div className="fc-activity-notification-item">
      <div className="fc-activity-notification-item__content">
        <div className="fc-activity-notification-item__time">
          <DateTime value={props.item.createdAt} />
        </div>
        <div className="fc-activity-notification-item__info">
          <div className="fc-activity-notification-item__type">
            { icon(props.item) }
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
};

export default NotificationItem;
