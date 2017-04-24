// @flow

import React from 'react';
import { Link } from 'react-router';

import styles from './list.css';

type CategoryLink = { label: string, to: string };
type Props = {
  category: CategoryLink,
  subCategories: Array<CategoryLink>,
};

const CategoryList = (props: Props) => {
  let links = props.subCategories.map((cat: CategoryLink) => {
    return (
      <li styleName="link">
        <Link to={cat.to}>
          {cat.label}
        </Link>
      </li>
    );
  });

  const shopAllLink = (
    <li styleName="shop-all-link">
      <Link to={props.category.to}>
        Shop All
      </Link>
    </li>
  );

  links = [...links, shopAllLink];

  return (
    <div styleName="container">
      <div styleName="title">{props.category.label}</div>
      <ul>
        {links}
      </ul>
    </div>
  );
};

export default CategoryList;
