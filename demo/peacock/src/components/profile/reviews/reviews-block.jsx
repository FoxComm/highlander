/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// components
import ErrorAlerts from 'ui/alerts/error-alerts';
import ReviewRow from './review-row';
import Modal from 'ui/modal/modal';
import ReviewForm from './review-form';

import * as actions from 'modules/reviews';

import type Review from 'types/review';

import styles from '../profile.css';

type Props = {
  auth: Object,
  reviews: Array<Review>,
  reviewsModalVisible: boolean,
};

type State = {
  error: ?string,
  currentReviewId: ?number,
};

class ReviewsBlock extends Component {
  props: Props;

  state: State = {
    error: null,
    currentReviewId: null,
  };

  componentWillMount() {
    this.fetchReviews().catch((error) => {
      this.setState({ error })
    });
  }

  @autobind
  fetchReviews() {
    if (_.get(this.props.auth, 'jwt')) {
      const userId = this.props.auth.user.id;
      return this.props.fetchReviewsForUser(userId);
    } else {
      return Promise.reject("Not authorized");
    }
  }

  get modal() {
    const { reviewsModalVisible } = this.props;

    return (
      <Modal
        show={reviewsModalVisible}
        toggle={this.props.toggleReviewsModal}
      >
        <ReviewForm
          reviewId={this.state.currentReviewId}
          closeModal={this.props.toggleReviewsModal}
          updateReview={this.props.updateReview}
          fetchReviews={this.fetchReviews}
          fetchState={this.props.reviewsState}
          updateReviewState={this.props.updateReviewState}
        />
      </Modal>
    );
  }

  @autobind
  handleReviewForm(reviewId) {
    this.props.toggleReviewsModal();
    this.setState({ currentReviewId: reviewId });
  }

  @autobind
  renderReview(review) {
    return (
      <ReviewRow
        review={review}
        key={`review-${review.id}`}
        handleReviewForm={this.handleReviewForm}
        removeReview={this.props.removeReview}
      />
    );
  }

  @autobind
  content(reviews) {
    const { reviewsState } = this.props;

    if (reviewsState.err || this.state.error) {
      return (
        <ErrorAlerts error={reviewsState.err || this.state.error} />
      );
    }

    return (
      <div styleName="reviews">
        {_.map(reviews, this.renderReview)}
      </div>
    );
  }

  @autobind
  myReviews(reviews, pending) {
    if (_.isEmpty(reviews)) return null;

    const title = pending ? "Review products" : "My reviews";

    return (
      <div styleName="reviews-block">
        <div styleName="title">{title}</div>
        <div styleName="divider table" />
        {this.content(reviews)}
      </div>
    );
  }

  get body() {
    const { reviews } = this.props;

    if (_.isEmpty(reviews)) return null;

    const notReviewed = _.filter(reviews, (review) => review.attributes.status.v == 'pending');
    const reviewed = _.difference(reviews, notReviewed);

    return (
      <div>
        {this.myReviews(reviewed, false)}
        {this.myReviews(notReviewed, true)}
        {this.modal}
      </div>
    );
  }

  render() {
    return this.body;
  }
}

const mapState = (state) => {
  return {
    reviews: _.get(state.reviews, 'list', []),
    auth: _.get(state, 'auth', {}),
    reviewsModalVisible: _.get(state.reviews, 'reviewsModalVisible', false),
    reviewsState: _.get(state.asyncActions, 'fetchReviews', {}),
    updateReviewState: _.get(state.asyncActions, 'updateReview', {}),
  };
};

export default connect(mapState, {
  ...actions,
})(ReviewsBlock);
