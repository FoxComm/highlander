/* @flow */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import _ from 'lodash';

// components
import { Dropdown } from 'components/dropdown';
import { PageTitle } from 'components/section-title';
import { PrimaryButton } from 'components/common/buttons';
import ContentBox from 'components/content-box/content-box';
import FormField from 'components/forms/formfield';
import FoxyForm from 'components/forms/foxy-form';
import WaitAnimation from 'components/common/wait-animation';

// redux
import * as applicationActions from 'modules/merchant-applications/details';

// styles
import styles from './details.css';

// types
import type { MerchantApplication, BusinessProfile, SocialProfile } from 'paragons/merchant-application';

const SELECT_STATE = [
  ['new', 'New', true],
  ['approved', 'Approved'],
  ['rejected', 'Rejected'],
  ['abandonded', 'Abandoned'],
];

type Props = {
  params: {
    applicationId: number,
  },
  details: {
    application: ?MerchantApplication,
    businessProfile: ?BusinessProfile,
    socialProfile: ?SocialProfile,
  },
  isFetching: boolean,
  fetchError: ?Object,
  fetchApplication: Function,
  fetchBusinessProfile: Function,
  fetchSocialProfile: Function,
  approveApplication: Function,
};

type State = {
  newState: string,
};

const mapStateToProps = (state) => {

  return {
    details: state.applications.details,
    isFetching: false,
    fetchError: _.get(state.asyncActions, 'getApplication.err', null),
  };
};

class MerchantApplicationDetails extends Component {
  props: Props;
  state: State;

  constructor(props: Props, ...args: any) {
    super(props, ...args);

    const { application } = props.details;
    if (application) {
      this.state = { newState: application.state };
    }
  }

  componentDidMount() {
    this.props.fetchApplication(this.props.params.applicationId);
    this.props.fetchBusinessProfile(this.props.params.applicationId);
    this.props.fetchSocialProfile(this.props.params.applicationId);
  }

  componentWillReceiveProps(nextProps: Props) {
    const { application } = nextProps.details;
    if (application) {
      this.setState({ newState: application.state });
    }
  }

  get isDirty(): boolean {
    const { application } = this.props.details;
    if (!application) {
      return false;
    }

    return this.state.newState != application.state;
  }


  get isStateEditable(): boolean {
    const state = _.get(this.props, 'details.application.state', '');
    return state == 'new';
  }

  get renderPageTitle(): ?Element<*> {
    if (this.props.details.application) {
      const title = this.props.details.application.business_name;
      return (
        <PageTitle title={title}>
          <PrimaryButton
            type="button"
            disabled={!this.isDirty}
            onClick={this.handleSubmit}>
            Save
          </PrimaryButton>
        </PageTitle>
      );
    }
  }

  get renderState() {
    return (
      <div className="fc-col-md-1-3">
        <ContentBox title="State">
          <Dropdown
            value={this.state.newState}
            onChange={this.handleStateChange}
            disabled={!this.isStateEditable}
            items={SELECT_STATE}
            changeable={false} />
        </ContentBox>
      </div>
    );
  }

  @autobind
  handleStateChange(newState) {
    this.setState({ newState });
  }

  @autobind
  handleSubmit() {
    const { application } = this.props.details;
    if (application) {
      // :( The Elixir app has camel cased JSON.
      const merchant_application = {
        ...application,
        state: this.state.newState,
      };

      this.props.approveApplication(application.id);
    }
  }

  render() {
    const { application, businessProfile, socialProfile } = this.props.details;
    const { isFetching, fetchError } = this.props;

    if (!application || !businessProfile || !socialProfile) {
      return (
        <div styleName="waiting">
          <WaitAnimation />
        </div>
      );
    }


    return (
      <div>
        {this.renderPageTitle}
        <div className="fc-grid">
          <div className="fc-col-md-2-3">
            <ContentBox title="Application">
              <div>
                <ul>
                  <li styleName="entry">
                    <div styleName="header">Business Name</div>
                    <div>{application.business_name}</div>
                  </li>
                  <li styleName="entry">
                    <div styleName="header">Reference Number</div>
                    <div>{application.reference_number}</div>
                  </li>
                  <li styleName="entry">
                    <div styleName="header">Email Address</div>
                    <div>{application.email_address}</div>
                  </li>
                  <li styleName="entry">
                    <div styleName="header">Monthly Sales Volume</div>
                    <div>{businessProfile.monthly_sales_volume}</div>
                  </li>
                  <li styleName="entry">
                    <div styleName="header">Categories</div>
                    <div>{businessProfile.categories.join(', ')}</div>
                  </li>
                  <li styleName="entry">
                    <div styleName="header">Target Audience</div>
                    <div>{businessProfile.target_audience.join(', ')}</div>
                  </li>
                  <li styleName="entry">
                    <div styleName="header">Twitter Handle</div>
                    <div>{socialProfile.twitter_handle}</div>
                  </li>
                </ul>
              </div>
            </ContentBox>
          </div>
          {this.renderState}
        </div>
      </div>
    );
  }
}

export default connect(mapStateToProps, applicationActions)(MerchantApplicationDetails);
