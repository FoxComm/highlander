'use strict';

import { List, Map } from 'immutable';
import BaseStore from './base-store';
import GiftCardConstants from '../constants/gift-card';

class GiftCardStore extends BaseStore {
  constructor() {
    super();
    this.changeEvent = 'change-gift-card';
    this.state = Map({
      giftCards: List([])
    });

    this.bindListener(GiftCardConstants.UPDATE_GIFT_CARDS, this.handleUpdateGiftCards);
    this.bindListener(GiftCardConstants.GIFT_CARD_FAILED, this.handleFailedGiftCards);
  }

  handleUpdateGiftCards(action) {
    // TODO: Get difference in cards and set 'new' property.
    let giftCards = action.giftCards;
    this.setState(this.state.set('giftCards', giftCards));
  }

  handleFailedGiftCards(action) {
    let errorMessage = action.errorMessage.trim();
    console.error(errorMessage);
  }
}

let giftCardStore = new GiftCardStore();
export default giftCardStore;
