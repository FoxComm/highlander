/* @flow weak */

// libs
import React, { Component, Element } from 'react';
import _ from 'lodash';

// components
import Loader from 'ui/loader';
import ActionLink from 'ui/action-link/action-link';

// styles
import styles from './product-reviews-list.css';

// types
type ReviewItem = {
  sku: string,
  id: number,
  scope: string,
  title: string,
  createdAt: string,
  updatedAt: string,
  body: string,
  archivedAt: ?string,
  userName: string,
}

type Props = {
  isLoading: ?boolean,
  loadingBehavior?: 0|1,
  listItems: ?Array<ReviewItem>,
  title: string,
  emptyContentTitle: string,
  paginationSize: number,
};

export const LoadingBehaviors = {
  ShowLoader: 1,
  ShowWrapper: 1,
};

const ReviewBody = (props): Element<*> => {
  const { title, userName, updatedAt, sku, body } = props;

  return (
    <div styleName="product-review-container">
      <div styleName="product-review-content">
        <div styleName="product-review-title">
          {title}
        </div>
        <div styleName="product-review-name-date">
          From: {userName} on {updatedAt}
        </div>
        <div styleName="product-review-variant">
          SKU: {sku}
        </div>
        <div styleName="product-review-body">
          {body}
        </div>
        <div styleName="product-review-flag">
          <ActionLink
            action={_.noop}
            title="Report Offensive Review"
            styleName="product-review-report-offense"
          />
        </div>
      </div>
    </div>
  );
};

class ProductReviewsList extends Component {

  props: Props;

  get reviewsEmptyContentTitle(): ?Element<*> {
    const { listItems, emptyContentTitle } = this.props;

    if (_.isEmpty(listItems)) {
      return (
        <div styleName="product-reviews-subtitle">
          {emptyContentTitle}
        </div>
      );
    }
    return null;
  }

  get displayReviews(): ?Element<*> {
    const { listItems } = this.props;

    if (!_.isEmpty(listItems)) {
      const reviews = _.map(listItems, (review) => {
        return (
          <ReviewBody
            key={review.id}
            title={review.title}
            userName={review.userName}
            updatedAt={review.updatedAt}
            sku={review.sku}
            body={review.body}
          />
        );
      });

      return (
        <div>
          {reviews}
          <ActionLink
            action={_.noop}
            title="LOAD MORE REVIEWS"
            styleName="product-review-load-more"
          />
        </div>
      );
    }
    return null;
  }

  render(): Element<*> {
    const { title, loadingBehavior = LoadingBehaviors.ShowLoader, isLoading } = this.props;

    if (loadingBehavior == LoadingBehaviors.ShowLoader && isLoading) {
      return <Loader styleName="" />;
    }

    return (
      <div styleName="product-reviews-list-wrapper">
        <div styleName="product-reviews-title">
          {title}
        </div>
        {this.reviewsEmptyContentTitle}
        {this.displayReviews}
      </div>
    );
  }
}

export default ProductReviewsList;
