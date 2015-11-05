'use strict';

import React, { PropTypes } from 'react';
import { PrimaryButton } from '../common/buttons';

export default class EditCreditCardBox extends React.Component {

  render() {
    return (
      <li className="fc-card-container fc-credit-cards fc-credit-cards-new">
        <div>
          Edit Credit Card
        </div>
        <div>
          <a className="fc-btn-link" onClick={ this.props.onCancel }>Cancel</a>
          <PrimaryButton>Save</PrimaryButton>
        </div>
      </li>
    );
  }
}
