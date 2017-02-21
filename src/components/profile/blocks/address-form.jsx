
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import styles from '../profile.css';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { browserHistory } from 'lib/history';

import Block from '../common/block';
import { Link } from 'react-router';
import Button from 'ui/buttons';
import EditAddress from 'ui/address/edit-address';
import makeLocalStore from 'lib/local-store';

import * as addressActions from 'modules/edit-address';
import addressReducer from 'modules/edit-address';
import * as checkoutActions from 'modules/checkout';

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

class AddressForm extends Component {
  static title = 'Add Address';

  state = {
    newAddress: null,
  };

  addressId(props) {
    return _.get(props.routeParams, 'addressId');
  }

  fetchAddress(props) {
    const addressId = this.addressId(props);
    if (addressId != 'new') {
      this.props.fetchAddress(addressId);
    }
  }

  componentWillMount() {
    this.fetchAddress(this.props);
  }

  componentWillReceiveProps(nextProps) {
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
  handleUpdateAddress(address) {
    this.setState({
      newAddress: address,
    });
  }

  render() {
    return (
      <Block title={AddressForm.title}>
        <EditAddress
          address={this.props.address}
          onUpdate={this.handleUpdateAddress}
          colorTheme="white-bg"
        />
        <div styleName="buttons-footer">
          <Button
            styleName="save-button"
            onClick={this.handleSave}
            isLoading={this.props.updateAddressState.inProgress}
          >
            Save
          </Button>
          <Link styleName="link" to="/profile">Cancel</Link>
        </div>
      </Block>
    );
  }
}

export default _.flowRight(
  connect(mapStateToProps, globalActions),
  makeLocalStore(addressReducer),
  connect(state => state, addressActions)
)(AddressForm);
