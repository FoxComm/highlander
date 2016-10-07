
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import styles from './checkout.css';
import { saveGiftCard } from 'modules/checkout';
import { connect } from 'react-redux';

import localized from 'lib/i18n';

import EditableBlock from 'ui/editable-block';
import { TextInput } from 'ui/inputs';
import Button from 'ui/buttons';
import { FormField } from 'ui/forms';

@localized
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
    const { t } = this.props;

    const code = this.state.code.replace(/\s+/g, '');

    this.props.saveGiftCard(code).then(() => {
      this.setState({code: ''});
    }).catch(() => {
      this.setState({code: '', error: t('Please enter a valid gift card and try again.')});
    });
  }

  render() {
    const { t } = this.props;

    return (
      <div styleName="gift-card-content">
        <FormField styleName="gift-card-code-field" error={this.state.error}>
          <TextInput
            styleName="gift-card-code"
            placeholder={t('CODE')}
            value={this.state.code}
            onChange={this.changeCode}
          />
        </FormField>
        <Button styleName="gift-card-submit" onClick={this.onSave}>
          {t('redeem')}
        </Button>
      </div>
    );
  }
}

const GiftCard = props => {
  const { t } = props;

  return (
    <EditableBlock
      styleName="checkout-block"
      title={t('GIFT CARD')}
      isEditing
      collapsed={false}
      content={<EditGiftCard saveGiftCard={props.saveGiftCard} />}
    />
  );
};

export default connect(null, { saveGiftCard })(localized(GiftCard));
