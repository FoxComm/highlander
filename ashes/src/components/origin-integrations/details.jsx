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
import * as originIntegrationActions from 'modules/origin-integrations/details';

// styles
import styles from './details.css';

// types
import type { OriginIntegration } from 'paragons/origin-integration';

type Props = {
  details: {
    originIntegration: ?OriginIntegration,
  },
  isFetching: boolean,
  fetchError: ?Object,
  fetchOriginIntegration: Function,
  updateOriginIntegration: Function,
};

type State = {
  shopify_key: string,
  shopify_password: string,
  shopify_domain: string,
};

const mapStateToProps = (state) => {
  return {
    details: state.originIntegrations.details,
    isFetching: _.get(state.asyncActions, 'getOriginIntegration.inProgress', true),
    fetchError: _.get(state.asyncActions, 'getOriginIntegration.err', null),
  };
};

class IntegrationDetails extends Component {
  props: Props;
  state: State = { shopify_key: '', shopify_password: '', shopify_domain: '' };

  componentDidMount() {
    const userId = getUserId();
    this.props.fetchOriginIntegration(userId);
  }

  componentWillReceiveProps(nextProps: Props) {
    const { originIntegration } = nextProps.details;
    if (originIntegration) {
      this.setState({
        shopify_key: originIntegration.shopify_key,
        shopify_password: originIntegration.shopify_password,
        shopify_domain: originIntegration.shopify_domain,
      });
    }
  }

  get isDirty(): Element {
    const { originIntegration } = this.props.details;
    if (!originIntegration) {
      return false;
    }

    return this.state.shopify_key !== originIntegration.shopify_key ||
      this.state.shopify_password !== originIntegration.shopify_password ||
      this.state.shopify_domain !== originIntegration.shopify_domain;
  }

  get renderPageTitle(): Element {
    return (
      <PageTitle title="Platform Integrations">
        <PrimaryButton
          type="button"
          disabled={!this.isDirty}
          onClick={this.handleSubmit}>
          Save
        </PrimaryButton>
      </PageTitle>
    );
  }


  @autobind
  handleSubmit() {
    const { originIntegration } = this.props.details;
    if (originIntegration) {
      // :( The Elixir app has camel cased JSON.
      const origin_integration = {
        origin_integration: {
          shopify_key: this.state.shopify_key,
          shopify_password: this.state.shopify_password,
          shopify_domain: this.state.shopify_domain,
        },
      };

      const userId = getUserId();
      this.props.updateOriginIntegration(userId, origin_integration);
    }
  }

  render(): Element {
    const { originIntegration } = this.props.details;
    const { isFetching, fetchError } = this.props;

    if (isFetching) {
      return (
        <div styleName="waiting">
          <WaitAnimation />
        </div>
      );
    }

    const handleShopifyKey = ({target}) => {
      this.setState({ shopify_key: target.value });
    };

    const handleShopifyPassword = ({target}) => {
      this.setState({ shopify_password: target.value });
    };

    const handleShopifyDomain = ({target}) => {
      this.setState({ shopify_domain: target.value });
    };

    return (
      <div>
        {this.renderPageTitle}
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            <ContentBox title="Shopify Credentials">
              <div>
                <ul>
                  <li styleName="entry">
                    <FormField label="Shopify Key" validator="ascii" maxLength={255}>
                      <div><input type="text" value={this.state.shopify_key} onChange={handleShopifyKey} /></div>
                    </FormField>
                  </li>
                  <li styleName="entry">
                    <FormField label="Shopify Password" validator="ascii" maxLength={255}>
                      <div><input type="text" value={this.state.shopify_password} onChange={handleShopifyPassword} /></div>
                    </FormField>
                  </li>
                  <li styleName="entry">
                    <FormField label="Shopify Domain" validator="ascii" maxLength={255}>
                      <div><input type="text" value={this.state.shopify_domain} onChange={handleShopifyDomain} /></div>
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

export default connect(mapStateToProps, originIntegrationActions)(IntegrationDetails);
