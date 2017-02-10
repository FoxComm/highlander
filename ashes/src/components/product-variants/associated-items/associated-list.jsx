// @flow
import _ from 'lodash';
import React, { Element } from 'react';
import styles from './associated-list.css';

import WaitAnimation from '../../common/wait-animation';

type ListItem = {
  key: string,
  image: string,
  title: string|Element,
  subtitle: string|Element,
}

type Props = {
  title: string|Element,
  list: Array<ListItem>,
  fetchState: AsyncState,
}

const AssociatedList = (props: Props) => {
  const renderItem = (item: ListItem) => {
    return (
      <li styleName="list-item" key={item.key}>
        <img styleName="item-image" src={item.image} />
        <div styleName="item-body">
          <div styleName="item-title">{item.title}</div>
          <div styleName="item-subtitle">{item.subtitle}</div>
        </div>
      </li>
    );
  };

  let content;

  if (_.get(props.fetchState, 'inProgress', null) !== false) {
    content = <WaitAnimation/>;
  } else {
    content = (
      <ul styleName="list">
        {_.map(props.list, renderItem)}
      </ul>
    );
  }

  return (
    <div styleName="root">
      <div styleName="title">{props.title}</div>
      {content}
    </div>
  );
};

export default AssociatedList;
