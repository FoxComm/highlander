
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';

import styles from '../profile.css';

import EditAddress from 'ui/address/edit-address';

class AddressForm extends Component {
  static title = 'Add Address';

  @autobind
  handleSave() {

  }

  render() {
    return (
      <div>
        <EditAddress/>
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
