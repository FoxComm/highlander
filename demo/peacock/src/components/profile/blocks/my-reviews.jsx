import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';

import Block from '../common/block';
import ErrorAlerts from '@foxcomm/wings/lib/ui/alerts/error-alerts';
import ReviewRow from './review-row';

import styles from '../profile.css';

type State = {
  error: ?string;
};

class MyReviews extends Component {
  state: State = {
    error: null,
  };

  @autobind
  renderReview(review) {
    return <ReviewRow review={review} />;
  }

  get content() {
    if (this.state.error) {
      return (
        <ErrorAlerts error={this.state.error} />
      );
    }
    const reviews = [
      {
        product: 'newly purchased product',
        date: undefined,
        status: 'Needs Review',
        isNew: true,
      },
      {
        product: 'previously reviewed product',
        date: '12 January, 2017',
        status: 'Reviewed',
        isNew: false,
      },
    ];
    return (
      <table styleName="simple-table">
        <thead>
          <tr>
            <th>Review Date</th>
            <th>Product</th>
            <th>Status</th>
            <th>&nbsp;</th>
          </tr>
        </thead>
        <tbody>
          {_.map(reviews, this.renderReview)}
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
