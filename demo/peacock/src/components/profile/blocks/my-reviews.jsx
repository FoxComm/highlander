// @flow
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

import Block from '../common/block';
import ErrorAlerts from '@foxcomm/wings/lib/ui/alerts/error-alerts';
import ReviewRow from './review-row';

import styles from '../profile.css';

import * as actions from 'modules/reviews';

function mapStateToProps(state) {
  return {
    reviews: _.get(state.reviews, 'list', []),
    auth: state.auth,
  };
}

type State = {
  error: ?string;
};

class MyReviews extends Component {
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

  render() {
    return (
      <Block title="My Reviews">
        {this.content}
      </Block>
    );
  }
}

export default connect(mapStateToProps, actions)(MyReviews);
