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
  details: {
    application: ?MerchantApplication,
    businessProfile: ?BusinessProfile,
    socialProfile: ?SocialProfile,
  },
  isFetching: boolean,
  fetchError: ?Object,
  fetchApplication: Function,
  fetchBusinessProfile: Function,
  fetchRealSocialProfile: Function,
  approveApplication: Function,
};

type State = {
  newState: string,
  instagram_handle: string,
  google_plus_handle: string,
  facebook_url: string,
};

const mapStateToProps = (state) => {

  return {
    details: state.applications.details,
    isFetching: false,
    fetchError: _.get(state.asyncActions, 'getSocialProfile.err', null),
  };
};

class ShopifyDetails extends Component {
  props: Props;
  state: State = { instagram_handle: '', google_plus_handle: '', facebook_url: '' };

  componentDidMount() {
    const userId = getUserId();
    this.props.fetchRealSocialProfile((userId % 9) + 2);
  }

  componentWillReceiveProps(nextProps: Props) {
    const { socialProfile } = nextProps.details;
    if (socialProfile) {
      this.setState({
        instagram_handle: socialProfile.instagram_handle,
        google_plus_handle: socialProfile.google_plus_handle,
        facebook_url: socialProfile.facebook_url,
      });
    }
  }

  get isDirty(): Element {
    const { socialProfile } = this.props.details;
    if (!socialProfile) {
      return false;
    }

    return this.state.instagram_handle !== socialProfile.instagram_handle ||
      this.state.google_plus_handle !== socialProfile.google_plus_handle ||
      this.state.facebook_url !== socialProfile.facebook_url;
  }

  get renderPageTitle(): Element {
    if (this.props.details.socialProfile) {
      return (
        <PageTitle title="Shopify Credentials">
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


  @autobind
  handleSubmit() {
    const { socialProfile } = this.props.details;
    if (socialProfile) {
      // :( The Elixir app has camel cased JSON.
      const social_profile = {
        social_profile: {
          instagram_handle: this.state.instagram_handle,
          google_plus_handle: this.state.google_plus_handle,
          facebook_url: this.state.facebook_url,
        },
      };

      const userId = (getUserId() % 9) + 2;
      this.props.updateSocialProfile(userId, social_profile);
    }
  }

  render(): Element {
    const { socialProfile } = this.props.details;
    const { isFetching, fetchError } = this.props;

    if (!socialProfile) {
      return (
        <div styleName="waiting">
          <WaitAnimation />
        </div>
      );
    }

    const handleShopifyKey = ({target}) => {
      this.setState({ instagram_handle: target.value });
    };

    const handleShopifyPassword = ({target}) => {
      this.setState({ google_plus_handle: target.value });
    };

    const handleShopifyDomain = ({target}) => {
      this.setState({ facebook_url: target.value });
    };

    return (
      <div>
        {this.renderPageTitle}
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            <ContentBox title="Enter Shopify Data">
              <div>
                <ul>
                  <li styleName="entry">
                    <FormField label="Shopify Key" validator="ascii" maxLength={255}>
                      <div><input type="text" value={this.state.instagram_handle} onChange={handleShopifyKey} /></div>
                    </FormField>
                  </li>
                  <li styleName="entry">
                    <FormField label="Shopify Password" validator="ascii" maxLength={255}>
                      <div><input type="text" value={this.state.google_plus_handle} onChange={handleShopifyPassword} /></div>
                    </FormField>
                  </li>
                  <li styleName="entry">
                    <FormField label="Shopify Domain" validator="ascii" maxLength={255}>
                      <div><input type="text" value={this.state.facebook_url} onChange={handleShopifyDomain} /></div>
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

export default connect(mapStateToProps, applicationActions)(ShopifyDetails);