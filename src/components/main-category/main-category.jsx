/* @flow */

import _ from 'lodash';
import React from 'react';
import type { HTMLElement } from 'types';
import { Link } from 'react-router';

import styles from './main-category.css';

type Props = {
  name: string;
  image: string;
  category: string;
}

const Banner = (props: Props): HTMLElement => (
  <article styleName="category">
    <h2 styleName="name">
      {props.name}
    </h2>
    <div styleName="links">
      {['men', 'women'].map(shop => (
        <Link
          key={`link-${props.category}-${shop}`}
          styleName="link"
          to={{pathname: props.category, query: {shop}}}
        >
          {`Shop ${_.capitalize(shop)}`}
        </Link>
      ))}
    </div>
    <div styleName="image">
      <img src={props.image} />
    </div>
  </article>
);

export default Banner;
