
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';

// components
import { DateTime } from '../common/datetime';
import UserInitials from '../users/initials';
import { IndexLink, Link } from '../link';

const icon = (item) => {
  const type = _.get(item, 'activityType');
  if (type.indexOf('order') >= 0) {
    return (<i className="icon icon-orders"></i>);
  } else if (type.indexOf('cart') >= 0) {
    return (<i className="icon icon-orders"></i>);
  } else if (type.indexOf('customer') >= 0) {
    return (<i className="icon icon-customers"></i>);
  } else if (type.indexOf('gift_card') >= 0) {
    return (<i className="icon icon-gift-cards"></i>);
  } else if (type.indexOf('store_credit') >= 0) {
    return (<i className="icon icon-store-credits"></i>);
  } else {
    return (<i className="icon icon-bell"></i>);
  }
};

const entity = (type) => {
  if (type.indexOf('order') >= 0) {
    return 'order';
  } else if (type.indexOf('cart') >= 0) {
    return 'cart for order';
  } else if (type.indexOf('customer') >= 0) {
    return 'customer';
  } else if (type.indexOf('gift_card') >= 0) {
    return 'gift-card';
  } else if (type.indexOf('store_credit') >= 0) {
    return 'store-credits';
  } else {
    return null;
  }
};

const buildDelimeter = (target) => {
  if (target.indexOf('assigned') >= 0) {
    return 'you to';
  } else if (target.indexOf('unassigned') >= 0) {
    return 'you from';
  } else {
    return null;
  }
};

const buildLink = (target, id) => {
  if (target.indexOf('order') >= 0) {
    return (
      <IndexLink to="order"
                 params={{order: id}}
                 className="fc-activity-notification-item__link">
        {id}
      </IndexLink>
    );
  } else if (target.indexOf('cart') >= 0) {
    return (
      <IndexLink to="order"
                 params={{order: id}}
                 className="fc-activity-notification-item__link">
        {id}
      </IndexLink>
    );
  } else if (target.indexOf('customer') >= 0) {
    return (
      <IndexLink to="customer"
                 params={{customerId: id}}
                 className="fc-activity-notification-item__link">
        {id}
      </IndexLink>
    );
  } else if (target.indexOf('gift_card') >= 0) {
    return (
      <IndexLink to="giftcard"
                 params={{giftCard: id}}
                 className="fc-activity-notification-item__link">
        {id}
      </IndexLink>
    );
  } else {
    return (<span>{ id }</span>);
  }
};

const action = (target) => {
  if (target.indexOf('created') >= 0) {
    return 'created';
  } else if (target.indexOf('updated') >= 0) {
    return 'updated';
  } else if (target.indexOf('deleted') >= 0) {
    return 'deleted';
  } else if (target.indexOf('assigned') >= 0) {
    return 'assigned';
  } else if (target.indexOf('unassigned') >= 0) {
    return 'unassigned';
  } else if (target.indexOf('removed') >= 0) {
    return 'removed';
  } else if (target.indexOf('changed') >= 0) {
    return 'changed';
  } else {
    return 'made unknown action';
  }
};

const buildText = (item) => {
  const target = _.get(item, 'activityType');
  const name = _.get(item, ['data', 'admin', 'name'], 'System');
  const actionText = action(target);
  const delimeter = buildDelimeter(target);
  const id = _.get(item, ['data', 'newInfo', 'id']);
  const link = buildLink(target, id);
  const entityText = entity(target);

  return (
    <span>
      {name} <strong>{actionText}</strong>{delimeter} {entityText} {link}
    </span>
  );
};

const NotificationItem = (props) => {
  const origin = _.get(props, ['item', 'data', 'admin']);
  const isRead = _.get(props, ['item', 'isRead']);
  const classes = classNames('fc-activity-notification-item', {
    '_not-read': !isRead
  });
  return (
    <div className={ classes }>
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
              { !_.isEmpty(origin) && (<UserInitials { ...origin } />) }
            </div>
            <div className="fc-activity-notification-item__text">
              { buildText(props.item) }
            </div>
          </div>
        </div>
      </div>
    </div>
  );
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
