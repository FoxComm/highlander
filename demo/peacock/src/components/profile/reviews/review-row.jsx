/* @flow */

import React from 'react';

// components
import ActionLink from 'ui/action-link/action-link';
import ProductImage from '../../image/image';

import type Review from 'types/review';

import styles from '../profile.css';

type Props = {
  review: Review,
  handleReviewForm: Function, // signature
};

const ReviewRow = (props: Props) => {
  const { review } = props;
  const { id, attributes } = review;

  const {
    imageUrl,
    productName,
    status
  } = attributes;

  const renderActions = () => {
    if (status.v == 'pending') {
      return (
        <div styleName="reviews-actions">
          <ActionLink
            action={() => { props.handleReviewForm(review.id); }}
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
          action={() => { props.handleReviewForm(review.id); }}
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
      {renderActions()}
    </div>
  );
};

export default ReviewRow;
