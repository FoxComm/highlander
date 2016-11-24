
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import styles from '../profile.css';
import { connect } from 'react-redux';

import { Link } from 'react-router';
import Button from 'ui/buttons';
import EditAddress from 'ui/address/edit-address';

class AddressForm extends Component {
  static title = 'Add Address';

  state = {
    address: {},
  };

  get addressId() {
    return _.get(this.props.routeParams, 'referenceNumber');
  }

  @autobind
  handleSave() {

  }

  render() {
    return (
      <div>
        <EditAddress address={}/>
        <div styleName="buttons-footer">
          <Button
            styleName="save-button"
            onClick={this.handleSave}
            isLoading={false}
          >
            Save
          </Button>
          <Link styleName="link" to="/profile">Cancel</Link>
        </div>
      </div>
    )
  }
}

export default AddressForm;
