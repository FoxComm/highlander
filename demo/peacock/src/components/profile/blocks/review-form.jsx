// @flow

import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router';
import { autobind } from 'core-decorators';

import Block from '../common/block';
import Button from 'ui/buttons';
import { FormField, Form } from 'ui/forms';
import { TextInput } from 'ui/text-input';
import { TextArea } from 'ui/textarea';

import * as actions from 'modules/reviews';

import styles from '../profile.css';

type Review = {
  sku: string,
  productName: string,
  id: number,
  status: 'pending' | 'submitted'
};

type FullReview = Review & {
  title: string,
  body: string,
}

type EmptyReview = Review & {
  title: void,
  body: void,
};

type State = {
  title: string,
  body: string,
  isHappy: boolean,
};

type ReviewFormProps = {
  review: FullReview | EmptyReview,
};

function mapStateToProps(state, props) {
  const reviewList = _.get(state, 'reviews.list', []);
  return {
    review: _.find(reviewList, review => review.id == props.routeParams.reviewId),
  };
}

class ReviewForm extends Component {
  static title = 'Edit Review';

  props: ReviewFormProps;
  state: State = {
    title: this.props.review.title || '',
    body: this.props.review.body || '',
    isHappy: true,
  }

  @autobind
  handleTitleChange(event) {
    this.setState({
      title: event.target.value,
    });
  }

  @autobind
  handleBodyChange(event) {
    this.setState({
      body: event.target.value,
    });
  }

  @autobind
  handleHappyClick(isHappy: boolean) {
    this.setState({
      isHappy,
    });
  }

  @autobind
  submitForm() {
    const payload = {
      sku: this.props.review.sku,
      attributes: {
        title: {
          t: 'string',
          v: this.state.title,
        },
        body: {
          t: 'string',
          v: this.state.body,
        },
        status: {
          t: 'string',
          v: 'submitted',
        },
      },
    };
    return this.props.updateReview(this.props.review.id, payload);
  }

  render() {
    return (
      <Block title={ReviewForm.title}>
        <Form onSubmit={this.submitForm}>
          <div styleName="section">Submit your review of {this.props.review.productName}</div>
          <FormField styleName="form-field">
            <TextInput
              required
              placeholder="Title of Review"
              value={this.state.title}
              onChange={this.handleTitleChange}
            />
          </FormField>
          <FormField>
            <TextArea
              required
              placeholder="Please add your product review here."
              value={this.state.body}
              onChange={this.handleBodyChange}
            />
          </FormField>
          <div styleName="buttons-footer">
            <Button type="submit" styleName="save-button" children="Save" />
            <Link styleName="link" to="/profile">Cancel</Link>
          </div>
        </Form>
      </Block>
    );
  }
}

export default connect(mapStateToProps, actions)(ReviewForm);
