
import React from 'react';

import { Link } from 'react-router';
import ProductImage from '../../image/image';

import styles from '../profile.css';

type Review = {
  sku: string,
  imageUrl: string,
  productName: string,
  id: number,
  status: 'pending' | 'submitted'
};

const renderActions: (string, Number) => Element<div> = (status, id) => {
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
  return (
    <div styleName="line-item">
      <div styleName="content">
        <div styleName="product-image">
          <ProductImage src={props.imageUrl} width={50} height={50} />
        </div>
        <div styleName="product-data">
          <div styleName="product-info">
            <div styleName="product-name">{props.productName}</div>
            <div styleName="product-variant">{/* TODO: variant info must be here */}</div>
          </div>
        </div>
        {renderActions(props.status, props.id)}
      </div>
    </div>
  );
};

export default ReviewRow;
