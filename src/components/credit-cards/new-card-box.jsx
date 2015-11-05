'use strict';

import React, { PropTypes } from 'react';
import { PrimaryButton } from '../common/buttons';
import { CheckBox } from '../checkbox/checkbox';

export default class NewCreditCardBox extends React.Component {

  render() {
    return (
      <li className="fc-card-container fc-credit-cards fc-credit-cards-new">
        <form>
          <div>
            New Credit Card
          </div>
          <div>

          </div>
          <div>
            <a className="fc-btn-link" onClick={ this.props.onCancel }>Cancel</a>
            <PrimaryButton type="submit">Save Customer</PrimaryButton>
          </div>
        </form>
      </li>
    );
  }
}
