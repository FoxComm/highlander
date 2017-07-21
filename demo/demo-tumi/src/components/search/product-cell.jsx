// @flow

import _ from 'lodash';
import React from 'react';
import { getESTaxonValue } from 'paragons/taxons';

import ProductImage from 'components/image/image';
import { Link } from 'react-router';
import ImagePlaceholder from 'components/products-item/image-placeholder';

import styles from './product-cell.css';

type Product = {
  model: {
    slug: ?string,
    id: number,
    title: string,
    retailPrice: number,
  },
};

export default (props: Product) => {
  const { model } = props;
  const imagePath = _.get(model, ['albums', 0, 'images', 0, 'src']);
  const image = imagePath ?
    <ProductImage src={imagePath} width={58} height={70} /> :
    <ImagePlaceholder />;

  const productSlug = model.slug != null && !_.isEmpty(model.slug) ? model.slug : model.id;
  const link = `/products/${productSlug}`;

  const collection = getESTaxonValue(model, 'collection');

  return (
    <Link to={link} styleName="block">
      <div styleName="image">
        {image}
      </div>
      <div styleName="container">
        <div styleName="product-name">
          {model.title}
        </div>
        <div styleName="category">
          {collection}
        </div>
      </div>
    </Link>
  );
};
