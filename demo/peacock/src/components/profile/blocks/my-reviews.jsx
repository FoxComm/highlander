import _ from 'lodash';
import React, { Component } from 'react';
import styles from '../profile.css';

import Block from '../common/block';

class MyReviews extends Component {
  get content() {
    const reviews = [(
      <tr>
        <td>placehoder</td>
        <td>placehoder</td>
        <td>placehoder</td>
        <td>placehoder</td>
      </tr>
    )];
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
          {_.map(reviews, _.identity)}
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
