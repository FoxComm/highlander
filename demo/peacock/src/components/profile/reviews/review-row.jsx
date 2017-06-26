/* @flow */

import React from 'react';

// components
import ActionLink from 'ui/action-link/action-link';
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
      <div styleName="reviews-actions">
        <ActionLink
          action={() => { console.log('add a review');}}
          title="Add review"
          styleName="reviews-action-link"
        />

        <ActionLink
          action={() => { console.log('ignore the review');}}
          title="Ignore"
          styleName="reviews-action-link"
        />
      </div>
    );
  }
  return (
    <div styleName="reviews-actions">
      <ActionLink
        action={() => { console.log('edit a review');}}
        title="Edit review"
        styleName="reviews-action-link"
      />

      <ActionLink
        action={() => { console.log('remove the review');}}
        title="Delete"
        styleName="reviews-action-link"
      />
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
