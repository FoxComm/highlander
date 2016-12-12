/* @flow */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import _ from 'lodash';
import { getUserId } from 'lib/claims';

// components
import { Dropdown } from 'components/dropdown';
import { PageTitle } from 'components/section-title';
import { PrimaryButton } from 'components/common/buttons';
import ContentBox from 'components/content-box/content-box';
import FormField from 'components/forms/formfield';
import SubNav from './sub-nav';
import WaitAnimation from 'components/common/wait-animation';

// redux
import * as originIntegrationActions from 'modules/origin-integrations/details';

// styles
import styles from './details.css';

// types
import type { ProductFeed } from 'paragons/origin-integration';

type Props = {
  details: {
    productFeed: ?ProductFeed,
  },
  isFetching: boolean,
  fetchError: ?Object,
  isCreating: boolean,
  createError: ?Object,
  isUpdating: boolean,
  isUpdating: ?Object,
  fetchProductFeed: Function,
  createProductFeed: Function,
  updateProductFeed: Function,
};

type State = ProductFeed;

const mapStateToProps = (state) => {
  return {
    details: state.originIntegrations.details,
    isFetching: _.get(state.asyncActions, 'getProductFeed.inProgress', true),
    fetchError: _.get(state.asyncActions, 'getProductFeed.err', null),
    isCreating: _.get(state.asyncActions, 'createProductFeed.inProgress', false),
    createError: _.get(state.asyncActions, 'createProductFeed.err', null),
    isUpdating: _.get(state.asyncActions, 'updateProductFeed.inProgress', false),
    updateError: _.get(state.asyncActions, 'updateProductFeed.err', null),
  };
};

class ProductFeedDetails extends Component {
  props: Props;
  state: State = {
    name: '',
    url: '',
    format: '',
    schedule: '',
  };

  componentDidMount() {
    const userId = getUserId();
    this.props.fetchProductFeed(userId);
  }

  componentWillReceiveProps(nextProps: Props) {
    const { productFeed } = nextProps.details;
    if (productFeed) {
      this.setState({ ...productFeed });
    }
  }

  get isDirty(): boolean {
    const { productFeed } = this.props.details;
    const name = _.get(productFeed, 'name', '');
    const url = _.get(productFeed, 'url', '');
    const format = _.get(productFeed, 'format', '');
    const schedule = _.get(productFeed, 'schedule', '');

    return this.state.name !== name ||
      this.state.url !== url ||
      this.state.format !== format ||
      this.state.schedule !== schedule;
  }

  get renderPageTitle(): Element {
    const { isCreating, isUpdating } = this.props;
    const isLoading = isCreating || isUpdating;
    const disabled = isLoading || !this.isDirty;

    return (
      <PageTitle title="Platform Integrations">
        <PrimaryButton
          type="button"
          disabled={disabled}
          isLoading={isLoading}
          onClick={this.handleSubmit}>
          Save
        </PrimaryButton>
      </PageTitle>
    );
  }


  @autobind
  handleSubmit() {
    const userId = getUserId();
    const product_feed = {
      product_feed: this.state,
    };

    const { productFeed } = this.props.details;
    if (productFeed) {
      this.props.updateProductFeed(userId, product_feed);
    } else {
      this.props.createProductFeed(userId, product_feed);
    }
  }

  render(): Element {
    const { productFeed } = this.props.details;
    const { isFetching, fetchError } = this.props;

    if (isFetching) {
      return (
        <div styleName="waiting">
          <WaitAnimation />
        </div>
      );
    }

    const handleName = ({target}) => {
      this.setState({ name: target.value });
    };

    const handleUrl = ({target}) => {
      this.setState({ url: target.value });
    };

    const handleFormat = ({target}) => {
      this.setState({ format: target.value });
    };

    const handleSchedule = ({target}) => {
      this.setState({ schedule: target.value });
    };

    return (
      <div>
        {this.renderPageTitle}
        <SubNav />
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            <ContentBox title="Product Feed Settings">
              <div>
                <ul>
                  <li styleName="entry">
                    <FormField
                      className="fc-object-form__field _form-field-required"
                      label="Feed Name"
                      validator="ascii"
                      maxLength={255}>
                      <input 
                        type="text"
                        className="fc-object-form__field-value"
                        value={this.state.name}
                        onChange={handleName} />
                    </FormField>
                  </li>
                  <li styleName="entry">
                    <FormField
                      className="fc-object-form__field _form-field-required"
                      label="Feed URL"
                      validator="ascii"
                      maxLength={255}>
                      <input
                        type="text"
                        className="fc-object-form__field-value"
                        value={this.state.url}
                        onChange={handleUrl} />
                    </FormField>
                  </li>
                  <li styleName="entry">
                    <FormField
                      className="fc-object-form__field _form-field-required"
                      label="Feed Format"
                      validator="ascii"
                      maxLength={255}>
                      <input
                        type="text"
                        className="fc-object-form__field-value"
                        value={this.state.format}
                        onChange={handleFormat} />
                    </FormField>
                  </li>
                  <li styleName="entry">
                    <FormField
                      className="fc-object-form__field _form-field-required"
                      label="Import Schedule"
                      validator="ascii"
                      maxLength={255}>
                      <input
                        type="text"
                        className="fc-object-form__field-value"
                        value={this.state.schedule}
                        onChange={handleSchedule} />
                    </FormField>
                  </li>
                </ul>
              </div>
            </ContentBox>
          </div>
        </div>
      </div>
    );
  }
}

export default connect(mapStateToProps, originIntegrationActions)(ProductFeedDetails);