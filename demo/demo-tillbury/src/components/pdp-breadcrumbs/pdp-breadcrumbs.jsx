/* @flow */

import React, { Element } from 'react';
import { Link } from 'react-router';
import styles from './pdp-breadcrumbs.css';

type Props = {
  category: {
    label: string,
    to: string,
  },
  subCategory?: {
    label: string,
    to: string,
  },
};

const ProductBreadcrumbs = (props: Props): Element<*> => {
  const { category, subCategory } = props;

  return (
    <div styleName="breadcrumb">
      <Link styleName="link" to="/">Home</Link>
      <span styleName="delimiter"> / </span>
      <Link to={category.to}>{category.label}</Link>
      {subCategory && (
        <span>
          <span styleName="delimiter"> / </span>
          <Link to={subCategory.to}>{subCategory.label}</Link>
        </span>
      )}
    </div>
  );
};

export default ProductBreadcrumbs;
