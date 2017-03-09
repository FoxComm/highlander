/* @flow */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import _ from 'lodash';
import { bindActionCreators } from 'redux';

// components
import { PageTitle } from 'components/section-title';
import { PrimaryButton } from 'components/common/buttons';
import ContentBox from 'components/content-box/content-box';
import FormField from 'components/forms/formfield';
import WaitAnimation from 'components/common/wait-animation';

// redux
import * as amazonActions from 'modules/channels/amazon';

// styles
import s from './amazon.css';

// types
// import type { OriginIntegration } from 'paragons/origin-integration';

type Props = {
  isFetching: boolean,
  credentials: ?Object,
};

type State = {
  seller_id: string,
  mws_auth_token: string,
};

function mapDispatchToProps(dispatch) {
  return {
    actions: bindActionCreators({
      ...amazonActions,
    }, dispatch),
  };
}

function mapStateToProps(state) {
  const { fetchAmazonCredentials = {}, updateAmazonCredentials = {} } = state.asyncActions;
  const { credentials } = state.channels.amazon;

  return {
    credentials,
    notReady: fetchAmazonCredentials.inProgress || !credentials,
    isFetching: fetchAmazonCredentials.inProgress,
    isPushing: updateAmazonCredentials.inProgress,
    fetchError: _.get(state.asyncActions, 'fetchAmazonCredentials.err', null),
    updateError: _.get(state.asyncActions, 'updateAmazonCredentials.err', null),
  };
};

class AmazonCredentials extends Component {
  props: Props;
  state: State = {
    seller_id: '',
    mws_auth_token: '',
  };

  componentDidMount() {
    this.props.actions.fetchAmazonCredentials();
  }

  componentWillReceiveProps(nextProps: Props) {
    const nextCredentials = nextProps.credentials;
    const { credentials } = this.props;

    if (!credentials && nextCredentials) {
      this.setState({
        seller_id: nextCredentials.seller_id,
        mws_auth_token: nextCredentials.mws_auth_token,
      });
    }
  }

  render() {
    const { notReady, isFetching, isPushing, fetchError, updateError } = this.props;

    if (notReady) {
      return <div><WaitAnimation className={s.waiting} /></div>;
    }

    if (fetchError) {
      return <div>fetchError</div>;
    }

    if (updateError) {
      return <div>updateError</div>;
    }

    return (
      <div>
        <PageTitle title="Amazon" />
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            <ContentBox title="Amazon Credentials">
              <ul>
                <li className={s.entry}>
                  <FormField label="Seller ID" validator="ascii" maxLength={255}>
                    <div>
                      <input
                        type="text"
                        value={this.state.seller_id}
                        onChange={(e) => this._handleSellerId(e)} />
                    </div>
                  </FormField>
                </li>
                <li className={s.entry}>
                  <FormField label="Auth token" validator="ascii" maxLength={255}>
                    <div>
                      <input
                        type="text"
                        value={this.state.mws_auth_token}
                        onChange={(e) => this._handleAuthToken(e)} />
                    </div>
                  </FormField>
                </li>
              </ul>
              <PrimaryButton
                type="button"
                disabled={isPushing}
                isLoading={isPushing}
                onClick={() => this._handleSubmit()}>
                Save
              </PrimaryButton>
            </ContentBox>
          </div>
        </div>
      </div>
    );
  }

  _handleSubmit() {
    const params = {
      seller_id: this.state.seller_id,
      mws_auth_token: this.state.mws_auth_token,
    };

    this.props.actions.updateAmazonCredentials(params);
  }

  _handleSellerId({ target }) {
    this.setState({ seller_id: target.value });
  };

  _handleAuthToken({ target }) {
    this.setState({ mws_auth_token: target.value });
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(AmazonCredentials);
