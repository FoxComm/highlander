'use strict';

import { List } from 'immutable';
import BaseStore from './base-store';
import GiftCardConstants from '../constants/gift-card';

class GiftCardStore extends BaseStore {
  constructor() {
    super();
    this.changeEvent = 'change-gift-card';
    this.state = List([]);

    this.bindListener(GiftCardConstants.UPDATE_GIFT_CARDS, this.handleUpdateGiftCards);
    this.bindListener(GiftCardConstants.GIFT_CARD_FAILED, this.handleFailedGiftCards);
    this.bindListener(GiftCardConstants.INSERT_GIFT_CARD, this.handleInsertGiftCard);
  }

  handleUpdateGiftCards(action) {
    // TODO: Get difference in cards and set 'new' property.
    this.setState(List(action.giftCards));
  }

  handleFailedGiftCards(action) {
    console.error(action.errorMessage.trim());
  }

  handleInsertGiftCard(action) {
    const giftCard = action.giftCard;
    this.setState(this.insertIntoList(this.state, giftCard, 'code'));
  }
}

let giftCardStore = new GiftCardStore();
export default giftCardStore;
