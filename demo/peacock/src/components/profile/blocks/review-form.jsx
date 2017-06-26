// @flow

import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// import Block from '../common/block';
// import Button from 'ui/buttons';
// import { FormField, Form } from 'ui/forms';
// import { TextInput } from 'ui/text-input';
// import { TextArea } from 'ui/textarea';
// import { Link } from 'react-router';
import * as actions from 'modules/reviews';

import styles from '../profile.css';

type ReviewAttributes = {
  imageUrl: {t: 'string', v: string},
  productName: {t: 'string', v: string},
  status: {t: 'string', v: string},
  title: {t: 'string', v: string},
  body: {t: 'string', v: string},
}

type Review = {
  sku: string,
  id: number,
  attributes: ReviewAttributes,
};

type State = {
  title: string,
  body: string,
  isHappy: boolean,
};

type ReviewFormProps = {
  review: Review,
  updateReview: Function,
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
    title: _.get(this.props.review, 'attributes.title.v', ''),
    body: _.get(this.props.review, 'attributes.body.v', ''),
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
        ...this.props.review.attributes,
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

/* TODO: implement the reviews on profile page according to the new design
<Block title={ReviewForm.title}>
  <Form onSubmit={this.submitForm}>
    <div styleName="section">Submit your review of {this.props.review.attributes.productName.v}</div>
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
*/
  render() {
    return <div />;
  }
}

export default connect(mapStateToProps, actions)(ReviewForm);
