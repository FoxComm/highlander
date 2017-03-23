/* @flow */

// libs
import React, { Component, Element } from 'react';
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

type Props = {
  isFetching: boolean,
  isPushing: boolean,
  isRemoving: boolean,
  inProgress: boolean,
  credentials: ?Object,
  actions: Object,
  fetchError: any,
  updateError: any,
  removeError: any,
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
  const {
    fetchAmazonCredentials = {},
    updateAmazonCredentials = {},
    removeAmazonCredentials = {},
  } = state.asyncActions;
  const { credentials } = state.channels.amazon;

  return {
    credentials,
    isFetching: fetchAmazonCredentials.inProgress,
    isPushing: updateAmazonCredentials.inProgress,
    isRemoving: removeAmazonCredentials.inProgress,
    fetchError: _.get(state.asyncActions, 'fetchAmazonCredentials.err', null),
    updateError: _.get(state.asyncActions, 'updateAmazonCredentials.err', null),
    removeError: _.get(state.asyncActions, 'removeAmazonCredentials.err', null),
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

    if (credentials != nextCredentials && nextCredentials) {
      this.setState({
        seller_id: nextCredentials.seller_id,
        mws_auth_token: nextCredentials.mws_auth_token,
      });
    }
  }

  render() {
    const { isFetching, isPushing, isRemoving, fetchError, updateError, removeError } = this.props;
    const inProgress = isFetching || isPushing || isRemoving;

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
                disabled={inProgress}
                isLoading={isPushing}
                onClick={() => this._handleSubmit()}>
                Save
              </PrimaryButton>

              <PrimaryButton
                className={s.remove}
                type="button"
                disabled={inProgress}
                isLoading={isRemoving}
                onClick={() => this._handleRemove()}>
                Remove
              </PrimaryButton>

              {isFetching &&
                <div className={s.preloader}>
                  <WaitAnimation className={s.waiting} size="m" />
                </div>
              }

              {updateError && <div>updateError</div>}
              {removeError && <div>removeError</div>}
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

  _handleRemove() {
    this.props.actions.removeAmazonCredentials().then(() => {
      this.setState({
        seller_id: '',
        mws_auth_token: '',
      });
    });
  }

  _handleSellerId({ target }) {
    this.setState({ seller_id: target.value });
  };

  _handleAuthToken({ target }) {
    this.setState({ mws_auth_token: target.value });
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(AmazonCredentials);
