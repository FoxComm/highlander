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

  get content() {
    if (this.state.error) {
      return (
        <ErrorAlerts error={this.state.error} />
      );
    }

    return (
      <div styleName="reviews">
        {_.map(this.props.reviews, this.renderReview)}
      </div>
    );
  }

  get body() {
    if (_.isEmpty(this.props.reviews)) return null;

    return (
      <div styleName="reviews-block">
        <div styleName="title">My reviews</div>
        <div styleName="divider table" />
        {this.content}
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
