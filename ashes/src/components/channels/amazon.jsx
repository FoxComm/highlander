/* @flow */

// libs
import React, { Component } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';
import { bindActionCreators } from 'redux';

// components
import { PageTitle } from 'components/section-title';
import { PrimaryButton } from 'components/core/button';
import ContentBox from 'components/content-box/content-box';
import FormField from 'components/forms/formfield';
import Spinner from 'components/core/spinner';

// redux
import * as amazonActions from 'modules/channels/amazon';

// styles
import s from './amazon.css';

type Props = {
  credentials: ?Object,
  actions: Object,
  fetchState: AsyncState,
  updateState: AsyncState,
  removeState: AsyncState,
};

type State = {
  seller_id: string,
  mws_auth_token: string,
};

function mapDispatchToProps(dispatch) {
  return {
    actions: bindActionCreators(
      {
        ...amazonActions,
      },
      dispatch
    ),
  };
}

function mapStateToProps(state) {
  const { credentials } = state.channels.amazon;

  return {
    credentials,
    fetchState: _.get(state.asyncActions, 'fetchAmazonCredentials', {}),
    updateState: _.get(state.asyncActions, 'updateAmazonCredentials', {}),
    removeState: _.get(state.asyncActions, 'removeAmazonCredentials', {}),
  };
}

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
    const { fetchState, updateState, removeState } = this.props;
    const inProgress = fetchState.inProgress || updateState.inProgress || removeState.inProgress;

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
                      <input type="text" value={this.state.seller_id} onChange={e => this._handleSellerId(e)} />
                    </div>
                  </FormField>
                </li>
                <li className={s.entry}>
                  <FormField label="Auth token" validator="ascii" maxLength={255}>
                    <div>
                      <input type="text" value={this.state.mws_auth_token} onChange={e => this._handleAuthToken(e)} />
                    </div>
                  </FormField>
                </li>
              </ul>
              <PrimaryButton
                type="button"
                disabled={inProgress}
                isLoading={updateState.inProgress}
                onClick={() => this._handleSubmit()}
              >
                Save
              </PrimaryButton>

              <PrimaryButton
                className={s.remove}
                type="button"
                disabled={inProgress}
                isLoading={removeState.inProgress}
                onClick={() => this._handleRemove()}
              >
                Remove
              </PrimaryButton>

              {fetchState.inProgress && <Spinner className={s.spinner} size="m" />}

              {updateState.err && <div>updateError</div>}
              {removeState.err && <div>removeError</div>}
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
  }

  _handleAuthToken({ target }) {
    this.setState({ mws_auth_token: target.value });
  }
}

export default connect(mapStateToProps, mapDispatchToProps)(AmazonCredentials);
