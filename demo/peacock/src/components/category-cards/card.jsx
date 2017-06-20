// @flow

import React from 'react';
import { Link } from 'react-router';

import styles from './card.css';

export type CardProps = {
  imageSrc: string,
  label: string,
  to: string,
};

const CategoryCard = (props: CardProps) => {
  return (
    <div styleName="card">
      <Link styleName="link" to={props.to}>
        <img styleName="image" alt={props.label} src={props.imageSrc} />
        <div styleName="label">{props.label}</div>
      </Link>
    </div>
  );
};

export default CategoryCard;
