
import React from 'react';

import { Link } from 'react-router';
import ProductImage from '../../image/image';

import styles from '../profile.css';

type ReviewAttributes = {
  imageUrl: {t: 'string', v: string},
  productName: {t: 'string', v: string},
  status: {t: 'string', v: string},
}

type Review = {
  sku: string,
  id: number,
  attributes: ReviewAttributes,
};

const renderActions: (string, number) => Element<*> = (status, id) => {
  if (status == 'pending') {
    return (
      <div styleName="actions-block">
        <Link styleName="link" to={`/profile/reviews/${String(id)}`}>ADD</Link>
        &nbsp;|&nbsp;
        <Link styleName="link" to={'/profile'}>IGNORE</Link>
      </div>
    );
  }
  return (
    <div styleName="actions-block">
      <Link styleName="link" to={`/profile/reviews/${String(id)}`}>EDIT</Link>
      &nbsp;|&nbsp;
      <Link styleName="link" to={'/profile'}>REMOVE</Link>
    </div>
  );
};

const ReviewRow = (props: Review) => {
  const { id } = props;
  const { imageUrl, productName, status } = props.attributes;
  return (
    <div styleName="reviews-content">
      <div styleName="product-data">
        <div styleName="product-image">
          <ProductImage src={imageUrl.v} width={50} height={50} />
        </div>
        <div styleName="product-info">
          <div styleName="product-name">{productName.v}</div>
          <div styleName="product-variant">{/* TODO: variant info must be here */}</div>
        </div>
      </div>
      {renderActions(status.v, id)}
    </div>
  );
};

export default ReviewRow;
