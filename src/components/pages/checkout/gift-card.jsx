
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import styles from './checkout.css';

import EditableBlock from 'ui/editable-block';
import { TextInput } from 'ui/inputs';
import Button from 'ui/buttons';

class EditGiftCard extends Component {

  state = {
    code: '',
  };

  @autobind
  changeCode({target}) {
    this.setState({
      code: target.value,
    });
  }

  render() {
    return (
      <div styleName="gift-card-content">
        <TextInput styleName="gift-card-code" placeholder="CODE" value={this.state.code} onChange={this.changeCode} />
        <Button styleName="gift-card-submit">REDEEM</Button>
      </div>
    );
  }
}

const GiftCard = () => {
  return (
    <EditableBlock
      styleName="checkout-block"
      title="GIFT CARD"
      isEditing
      collapsed={false}
      content={<EditGiftCard />}
    />
  );
};

export default GiftCard;
