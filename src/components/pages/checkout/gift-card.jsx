
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import styles from './checkout.css';
import { saveGiftCard } from 'modules/checkout';
import { connect } from 'react-redux';

import EditableBlock from 'ui/editable-block';
import { TextInput } from 'ui/inputs';
import Button from 'ui/buttons';
import { FormField } from 'ui/forms';

class EditGiftCard extends Component {

  state = {
    code: '',
    error: false,
  };

  @autobind
  changeCode({target}) {
    this.setState({
      code: target.value,
      error: false,
    });
  }

  @autobind
  onSave() {
    this.props.saveGiftCard(this.state.code).then(() => {
      this.setState({code: ''});
    }).catch(() => {
      console.log('in catch');
      this.setState({code: '', error: 'Please enter a valid gift card and try again.'});
    });
  }

  render() {
    return (
      <div styleName="gift-card-content">
        <FormField styleName="gift-card-code-field" error={this.state.error}>
          <TextInput styleName="gift-card-code" placeholder="CODE" value={this.state.code} onChange={this.changeCode} />
        </FormField>
        <Button styleName="gift-card-submit" onClick={this.onSave}>
          redeem
        </Button>
      </div>
    );
  }
}

const GiftCard = props => {
  return (
    <EditableBlock
      styleName="checkout-block"
      title="GIFT CARD"
      isEditing
      collapsed={false}
      content={<EditGiftCard saveGiftCard={props.saveGiftCard} />}
    />
  );
};

export default connect(null, { saveGiftCard })(GiftCard);
