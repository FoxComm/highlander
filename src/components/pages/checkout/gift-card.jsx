
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import styles from './checkout.css';

import EditableBlock from 'ui/editable-block';
import { TextInput } from 'ui/inputs';

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
      <div styleName="checkout-block-content">
        <TextInput placeholder="CODE" value={this.state.code} onChange={this.changeCode} />
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
