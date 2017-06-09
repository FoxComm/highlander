// @flow

// libs
import _ from 'lodash';
import React from 'react';
import classNames from 'classnames';

// components
import { DateTime } from '../common/datetime';
import AuthorTitle from '../activity-trail/activities/base/author-title';
import AuthorIcon from '../activity-trail/activities/base/author-icon';
import { representatives } from '../activity-trail/activities/index';
import { processActivity } from '../../modules/activity-trail';

// styles
import s from './item.css';

type Props = {
  item: Object;
};

function getIcon(type) {
  let postfix = '';

  switch (true) {
    case type.includes('order'):
    case type.includes('cart'):
      postfix = 'orders';
      break;
    case type.includes('user'):
    case type.includes('customer'):
      postfix = 'customers';
      break;
    case type.includes('gift_card'):
    case type.includes('store_credit'):
      postfix = 'gift-cards';
      break;
    default:
      postfix = 'bell';
  }

  return <i className={`icon icon-${postfix}`} />;
}

const NotificationItem = (props: Props) => {
  const isRead = _.get(props, ['item', 'isRead']);
  const classes = classNames(s.block, {
    [s.notRead]: !isRead
  });
  const activity = processActivity(props.item);
  const desc = representatives[activity.kind];

  if (!desc) return null;

  const title = desc.title(activity.data, activity);

  return (
    <div className={classes}>
      <div className={s.time}>
        <DateTime value={props.item.createdAt} />
      </div>
      <div className={s.info}>
        <div className={s.type}>
          {getIcon(props.item.kind)}
        </div>
        <div className={s.body}>
          <AuthorIcon activity={activity} className={s.icon} />
          <span>
            <AuthorTitle activity={activity} />&nbsp;{title}
          </span>
        </div>
      </div>
    </div>
  );
};

export default NotificationItem;
