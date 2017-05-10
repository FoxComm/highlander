import _ from 'lodash';
import React, { Component } from 'react';
import styles from '../profile.css';

import Block from '../common/block';
import ErrorAlerts from '@foxcomm/wings/lib/ui/alerts/error-alerts';
import ReviewRow from './review-row';

type State = {
  error: ?string;
};

class MyReviews extends Component {
  state: State = {
    error: null,
  };

  get content() {
    if (this.state.error) {
      return (
        <ErrorAlerts error={this.state.error} />
      );
    }
    const reviews = [{
      product: 'placeholder product',
      date: 'today',
      status: 'status',
      rating: '4',
    }];
    return (
      <table styleName="simple-table">
        <thead>
          <tr>
            <th>Product</th>
            <th>Review Date</th>
            <th>Status</th>
            <th>Rating</th>
            <th>&nbsp;</th>
          </tr>
        </thead>
        <tbody>
          {_.map(reviews, (review) => <ReviewRow review={review} />)}
        </tbody>
      </table>
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

export default MyReviews;
