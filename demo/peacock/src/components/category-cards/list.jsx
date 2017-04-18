// @flow

import React from 'react';
import CategoryCard from './card';

import styles from './list.css';

import type { CardProps } from './card';

type Props = {
  categories: Array<CardProps>,
};

const CardList = (props: Props) => {
  const list = props.categories.map((category: CardProps) => {
    return (
      <li styleName="item" key={category.label}>
        <CategoryCard {...category} />
      </li>
    );
  });

  return (
    <ul styleName="list">
      {list}
    </ul>
  );
};

export default CardList;
