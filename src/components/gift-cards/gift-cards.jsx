// libs
import React from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';

// styles
import styles from './gift-cards.css';

// components
import { TextInput } from 'ui/inputs';
import { FormField } from 'ui/forms';
import AddToCartBtn from 'ui/add-to-cart-btn';
import Autocomplete from 'ui/autocomplete';

function mapStateToProps(state) {
  return {

  };
}

export class GiftCards extends React.Component {
  state = {};

  render() {
    return (
      <div styleName="container">
        <div styleName="card-image">
          <div styleName="image"><div>Gift Card</div></div>
        </div>
        <div styleName="card-form">
          <div styleName="card-form-wrap">
            <h1 styleName="title">Digital Gift Card</h1>
            <div styleName="description">
              Give the gift of delicious food! To email the gift card on a specific date, select it below. To email immediately, select today.
            </div>

            <FormField styleName="form" error={this.state.error}>
              <div styleName="price-selector">
                <Autocomplete
                  inputProps={{
                    type: 'text',
                  }}
                  getItemValue={_.identity}
                  items={['$10', '$25', '$50', '$100']}
                  onSelect={_.noop}
                  selectedItem={this.state.quantity}
                  sortItems={false}
                />
              </div>
              <TextInput
                styleName="input-field"
                value={''}
                placeholder="Recipient name"
                onChange={null}
              />
              <TextInput
                styleName="input-field"
                value={''}
                placeholder="Recipient email"
                onChange={null}
              />
              <textarea
                styleName="message-field"
                placeholder="Your message"
              />
              <TextInput
                styleName="input-field"
                value={''}
                placeholder="Sender name"
                onChange={null}
              />
              <TextInput
                styleName="input-field"
                value={''}
                placeholder="Deliviry date"
                onChange={null}
              />
              <AddToCartBtn expanded styleName="add-to-cart-btn" />
            </FormField>
          </div>
        </div>
      </div>
    );
  }
}

export default connect(
  mapStateToProps,
// Implement map dispatch to props
)(GiftCards);
