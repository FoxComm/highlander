/* @flow */

import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

import ErrorAlerts from 'ui/alerts/error-alerts';
import ReviewRow from './review-row';

import styles from '../profile.css';

import * as actions from 'modules/reviews';

type Props = {
  auth: Object,
  reviews: Array<Object>,
};

type State = {
  error: ?string;
};

class ReviewsBlock extends Component {
  props: Props;

  state: State = {
    error: null,
  };

  componentWillMount() {
    if (_.get(this.props.auth, 'jwt')) {
      const userId = this.props.auth.user.id;
      this.props.fetchReviewsForUser(userId).catch((ex) => {
        this.setState({
          error: ex.toString(),
        });
      });
    }
  }

  @autobind
  renderReview(review) {
    return <ReviewRow {...review} key={`review-${review.id}`} />;
  }

  @autobind
  content(reviews) {
    if (this.state.error) {
      return (
        <ErrorAlerts error={this.state.error} />
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
  };
};

export default connect(mapState, {
  ...actions,
})(ReviewsBlock);
