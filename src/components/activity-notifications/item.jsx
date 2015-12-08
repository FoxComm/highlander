import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classNames';
import { DateTime } from '../common/datetime';
import UserInitials from '../users/initials';
import { IndexLink, Link } from '../link';

const icon = (item) => {
  const type = _.get(item, ['body', 'reference', 'typed']);
  switch(type) {
    case 'Order':
      return (<i className='icon icon-orders'></i>);
    default:
      return (<i className='icon icon-bell'></i>);
  }
};

/**
* Bob Saget changed the email address on Customer 541543252.
* You assigned yourself to Order 541543252.
* Bree swineford unassigned you from Order 541543252.
* Bree swineford assigned you to Order 541543252.
*/

const buildDelimeter = (action) => {
  switch(action) {
    case 'assigned':
      return ' you to ';
    case 'unassigned':
      return ' you from ';
    default:
      return ' on ';
  }
};

const buildLink = (target, id) => {
  switch(target) {
    case 'Order':
      return (
        <IndexLink to="order"
                   params={{order: id}}
                   className="fc-activity-notification-item__link">
          {id}
        </IndexLink>
      );
    default:
      return (<span>{ id }</span>);
  }
};

const buildText = (item) => {
  const target = _.get(item, ['body', 'reference', 'typed']);
  const name = _.get(item, ['body', 'origin', 'name'], 'System');
  const action = _.get(item, ['body', 'action']);
  const delimeter = buildDelimeter(action);
  const id = _.get(item, ['body', 'reference', 'ref']);
  const link = buildLink(target, id);

  return (
    <span>
      { name } <strong>{ action }</strong>{ delimeter } { target } { link }
    </span>
  );
};

const NotificationItem = (props) => {
  const origin = _.get(props, ['item', 'body', 'origin']);
  const isRead = _.get(props, ['item', 'isRead']);
  console.log(isRead);
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
              { !_.isEmpty(origin) && (<UserInitials model={ props.item.body.origin } />) }
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
      origin: PropTypes.obj
    })
  })
};

export default NotificationItem;
