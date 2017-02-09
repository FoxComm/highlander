// @flow
import _ from 'lodash';
import React, { Element } from 'react';
import styles from './associated-list.css';

type ListItem = {
  image: string,
  title: string|Element,
  subtitle: string|Element,
}

type Props = {
  title: string|Element,
  list: Array<ListItem>,
}

const AssociatedList = (props: Props) => {
  const renderItem = (item: ListItem) => {
    return (
      <li styleName="list-item">
        <img styleName="item-image" src={item.image} />
        <div styleName="item-body">
          <div styleName="item-title">{item.title}</div>
          <div styleName="item-subtitle">{item.subtitle}</div>
        </div>
      </li>
    );
  };

  return (
    <div styleName="root">
      <div styleName="title">{props.title}</div>
      <ul styleName="list">
        {_.map(props.list, renderItem)}
      </ul>
    </div>
  );
};

export default AssociatedList;
