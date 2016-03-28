
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import styles from './checkout.css';
import { saveGiftCard } from 'modules/checkout';
import { connect } from 'react-redux';

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

  @autobind
  onSave() {
    this.props.saveGiftCard(this.state.code).catch(() => {
      this.setState({code: ''});
    });
  }

  render() {
    return (
      <div styleName="gift-card-content">
        <TextInput styleName="gift-card-code" placeholder="CODE" value={this.state.code} onChange={this.changeCode} />
        <Button styleName="gift-card-submit" onClick={this.onSave}>
          REDEEM
        </Button>
      </div>
    );
  }
}

const GiftCard = props => {
  const saveFun = props.saveGiftCard;
  return (
    <EditableBlock
      styleName="checkout-block"
      title="GIFT CARD"
      isEditing
      collapsed={false}
      content={<EditGiftCard saveGiftCard={saveFun} />}
    />
  );
};

export default connect(null, { saveGiftCard })(GiftCard);
