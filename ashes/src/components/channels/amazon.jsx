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

// type Props = {
//   details: {
//     originIntegration: ?OriginIntegration,
//   },
//   isFetching: boolean,
//   fetchError: ?Object,
//   isCreating: boolean,
//   createError: ?Object,
//   isUpdating: boolean,
//   isUpdating: ?Object,
//   fetchOriginIntegration: Function,
//   createOriginIntegration: Function,
//   updateOriginIntegration: Function,
// };

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
  const aa = state.asyncActions;
  const { credentials } = state.channels.amazon;

  return {
    credentials,
    isFetching: aa.fetchAmazonCredentials && aa.fetchAmazonCredentials.inProgress || !credentials,
    // isFetching: _.get(state.asyncActions, 'getOriginIntegration.inProgress', true),
    // fetchError: _.get(state.asyncActions, 'getOriginIntegration.err', null),
    // isCreating: _.get(state.asyncActions, 'createOriginIntegration.inProgress', false),
    // createError: _.get(state.asyncActions, 'createOriginIntegration.err', null),
    // isUpdating: _.get(state.asyncActions, 'updateOriginIntegration.inProgress', false),
    // updateError: _.get(state.asyncActions, 'updateOriginIntegration.err', null),
  };
};

class AmazonCredentials extends Component {
  // props: Props;
  state: State = {
    seller_id: '',
    mws_auth_token: '',
  };

  componentDidMount() {
    this.props.actions.fetchAmazonCredentials();
  }

  componentWillReceiveProps(nextProps) {
    const nextCredentials = nextProps.credentials;
    const { credentials } = this.props;

    if (!credentials && nextCredentials) {
      this.setState({
        seller_id: nextCredentials.seller_id,
        mws_auth_token: nextCredentials.mws_auth_token,
      });
    }
  }

  // get isDirty(): boolean {
  //   const { originIntegration } = this.props.details;
  //   const key = _.get(originIntegration, 'client_id', '');
  //   const password = _.get(originIntegration, 'seller_id', '');
  //   const domain = _.get(originIntegration, 'mws_auth_token', '');

  //   return this.state.client_id !== key ||
  //     this.state.seller_id !== password ||
  //     this.state.mws_auth_token !== domain;
  // }

  @autobind
  handleSubmit() {
    const params = {
      seller_id: this.state.seller_id,
      mws_auth_token: this.state.mws_auth_token,
    };

    this.props.actions.updateAmazonCredentials(params);
  }

  render() {
    const { isFetching } = this.props;

    // @todo button stuff
    // const { isCreating, isUpdating } = this.props;
    // const isLoading = isCreating || isUpdating;
    // const disabled = isLoading || !this.isDirty;

    const disabled = false;
    const isLoading = false;

    if (isFetching) {
      return <div><WaitAnimation className={s.waiting} /></div>;
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
                disabled={disabled}
                isLoading={isLoading}
                onClick={this.handleSubmit}>
                Save
              </PrimaryButton>
            </ContentBox>
          </div>
        </div>
      </div>
    );
  }

  _handleSellerId({ target }) {
    this.setState({ seller_id: target.value });
  };

  _handleAuthToken({ target }) {
    this.setState({ mws_auth_token: target.value });
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(AmazonCredentials);
