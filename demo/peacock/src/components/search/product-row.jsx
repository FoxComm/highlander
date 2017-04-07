// @flow

import _ from 'lodash';
import React from 'react';

import Currency from 'ui/currency';
import ProductImage from 'components/image/image';
import { Link } from 'react-router';

import styles from './product-row.css';

export default (props) => {
  console.log(props);
  const { model } = props;
  const imagePath = _.get(model, ['albums', 0, 'images', 0, 'src']);
  const image = imagePath ?
        <ProductImage src={imagePath} width={50} height={50} /> :
        <ImagePlaceholder />;

  return (
    <Link to="" styleName="link">
      <div styleName="box">
        <div styleName="image">
          {image}
        </div>
        <div styleName="container">
          <div styleName="product-name">
            {model.title}
          </div>
          <div styleName="price">
            <Currency value={model.retailPrice} />
          </div>
        </div>
      </div>
    </Link>
  );
};
