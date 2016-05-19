/* @flow */

import _ from 'lodash';
import React from 'react';
import type { HTMLElement } from 'types';
import { Link } from 'react-router';

import styles from './category.css';

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
    <img styleName="image" src={props.image} />
    <div styleName="links">
      {['men', 'women'].map(type => (
        <Link
          key={`link-${props.category}-${type}`}
          styleName="link"
          to={{pathname: `/${props.category}`, query: {type}}}
        >
          {`Shop ${_.capitalize(type)}`}
        </Link>
      ))}
    </div>
  </article>
);

export default Banner;
