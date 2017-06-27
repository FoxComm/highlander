// @flow

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';

// components
import CheckoutForm from 'pages/checkout/checkout-form';
import { FormField } from 'ui/forms';
import { TextInput } from 'ui/text-input';
import { TextArea } from 'ui/textarea';
import Loader from 'ui/loader';
import ProductImage from 'components/image/image';
import RadioButton from 'ui/radiobutton/radiobutton';

// types
import type Review from 'types/review';

import styles from '../profile.css';

type State = {
  title: string,
  body: string,
  isHappy: boolean,
  currentReview: ?Review,
};

type Props = {
  reviewId: ?number,
  updateReview: Function, // signature
  closeModal: Function, // signature
  updateReviewState: Object, // AsyncActions type
  fetchState: Object, // AsyncActions type
  fetchReviews: Function, // signature
  updateReviewState: Object, // AsyncActions type
};

class ReviewForm extends Component {
  props: Props;

  state: State = {
    title: '',
    body: '',
    isHappy: true,
    currentReview: null,
  }

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.reviewId != this.props.reviewId) {
      this.props.fetchReviews().then((resp) => {
        const currentReview = _.find(resp.result, { id: nextProps.reviewId });
        this.setState({
          title: _.get(currentReview, 'attributes.title.v', ''),
          body: _.get(currentReview, 'attributes.body.v', ''),
          isHappy: true, // not implemented yet on backend
          currentReview: _.isEmpty(currentReview) ? null : currentReview,
        });
      });
    }
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

/* Ratings are not implemented yet on backend
  @autobind
  handleRatingChange() {
    const currentRating = this.state.isHappy;

    this.setState({
      isHappy: !currentRating,
    });
  }
*/

  @autobind
  submitForm() {
    const { currentReview } = this.state;

    const payload = {
      sku: currentReview.sku,
      attributes: {
        ...currentReview.attributes,
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
    return this.props.updateReview(currentReview.id, payload).then(() => {
      this.props.closeModal();
      this.props.fetchReviews();
    });
  }

  get productImage() {
    const { currentReview } = this.state;

    if (_.isEmpty(currentReview)) return null;

    const { imageUrl, productName } = currentReview.attributes;

    return (
      <div styleName="product-info modal">
        <div styleName="product-image modal">
          <ProductImage src={imageUrl.v} width={50} height={50} />
        </div>
        <div styleName="product-name modal">
          {productName.v}
        </div>
      </div>
    );
  }

/* Ratings are not implemented yet on backend
  get rating() {
    return (
      <div styleName="rating">
        <RadioButton
          name="rating"
          checked={this.state.isHappy}
          onChange={this.handleRatingChange}
          id="rating-happy"
          styleName="rating-button"
        >
          Happy with my purchase
        </RadioButton>
        <RadioButton
          name="rating"
          checked={!this.state.isHappy}
          onChange={this.handleRatingChange}
          id="rating-disappointed"
          styleName="rating-button"
        >
          Dissappointed with my purchase
        </RadioButton>
      </div>
    );
  }
  */

  get content() {
    const { fetchState } = this.props;

    if (fetchState.inProgress) {
      return (
        <Loader />
      );
    }

    return (
      <div>
        {this.productImage}
        <FormField styleName="form-field">
          <TextInput
            required
            placeholder="Review title"
            value={this.state.title}
            onChange={this.handleTitleChange}
          />
        </FormField>
        <FormField styleName="form-field">
          <TextArea
            required
            placeholder="Product review"
            value={this.state.body}
            onChange={this.handleBodyChange}
            styleName="review-body"
          />
        </FormField>
        {/* this.rating */}
      </div>
    );
  }

  get modalTitle() {
    const { currentReview } = this.state;
    const { fetchState } = this.props;

    if (fetchState.inProgress) return null;

    if (currentReview && currentReview.attributes.status.v === 'pending') return "New review";

    return "Edit review";
  }

  render() {
    const action = {
      handler: this.props.closeModal,
      title: 'Cancel',
    };

    const disabled = !this.state.title && !this.state.body;

    return (
      <CheckoutForm
        error={this.props.updateReviewState.err || this.props.fetchState.err}
        inProgress={this.props.updateReviewState.inProgress}
        submit={this.submitForm}
        buttonLabel="Save"
        title={this.modalTitle}
        action={action}
        buttonDisabled={disabled}
      >
        {this.content}
      </CheckoutForm>
    );
  }
}


export default ReviewForm;
