// @flow weak
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import styles from '../profile.css';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { browserHistory } from 'lib/history';
import sanitizeAddresses from 'sanitizers/addresses';

import { Form } from 'ui/forms';
import Block from '../common/block';
import { Link } from 'react-router';
import Button from 'ui/buttons';
import EditAddress from 'ui/address/edit-address';
import makeLocalStore from 'lib/local-store';
import ErrorAlerts from '@foxcomm/wings/lib/ui/alerts/error-alerts';

import * as addressActions from 'modules/edit-address';
import addressReducer from 'modules/edit-address';
import * as checkoutActions from 'modules/checkout';

import type { Address } from 'types/address';
import type { AsyncStatus } from 'types/async-actions';

function mapStateToProps(state) {
  return {
    updateAddressState: _.get(state.asyncActions, 'updateAddress', {}),
  };
}

function globalActions(dispatch) {
  return {
    checkoutActions: bindActionCreators(checkoutActions, dispatch),
  };
}

type Props = {
  updateAddressState: AsyncStatus,
  routeParams: Object,
  fetchAddress: (addressId: number) => Promise,
  checkoutActions: {
    updateAddress: (address: Address, id?: number) => Promise,
  },
  address: Address|void,
}

type State = {
  newAddress: Address|null,
}

class AddressForm extends Component {
  props: Props;

  static title = 'Add Address';

  state: State = {
    newAddress: null,
  };

  addressId(props: Props) {
    return _.get(props.routeParams, 'addressId');
  }

  fetchAddress(props: Props) {
    const addressId = this.addressId(props);
    if (addressId != 'new') {
      this.props.fetchAddress(addressId);
    }
  }

  componentWillMount() {
    this.fetchAddress(this.props);
  }

  componentWillReceiveProps(nextProps: Props) {
    if (this.addressId(this.props) != this.addressId(nextProps)) {
      this.fetchAddress(nextProps);
    }
  }

  @autobind
  handleSave() {
    const addressId = this.addressId(this.props);
    const newAddress = this.state.newAddress || this.props.address;
    this.props.checkoutActions.updateAddress(
      newAddress,
      addressId != 'new' ? addressId : void 0
    ).then(() => {
      browserHistory.push('/profile');
    });
  }

  @autobind
  handleUpdateAddress(address: Address) {
    this.setState({
      newAddress: address,
    });
  }

  render() {
    return (
      <Block title={AddressForm.title}>
        <Form onSubmit={this.handleSave}>
          <EditAddress
            address={this.props.address}
            onUpdate={this.handleUpdateAddress}
            colorTheme="white-bg"
          />
          <ErrorAlerts
            error={this.props.updateAddressState.err}
            sanitizeError={sanitizeAddresses}
          />
          <div styleName="buttons-footer">
            <Button
              type="submit"
              styleName="save-button"
              isLoading={this.props.updateAddressState.inProgress}
              children="Save"
            />
            <Link styleName="link" to="/profile">Cancel</Link>
          </div>
        </Form>
      </Block>
    );
  }
}

export default _.flowRight(
  connect(mapStateToProps, globalActions),
  makeLocalStore(addressReducer),
  connect(state => state, addressActions)
)(AddressForm);
