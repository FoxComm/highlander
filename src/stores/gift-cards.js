'use strict';

import alt from '../alt';
import GiftCardActions from '../actions/gift-cards';

class GiftCardStore {
  constructor() {
    this.giftCards = [];

    this.bindListeners({
      handleUpdateGiftCards: GiftCardActions.UPDATE_GIFT_CARDS,
      handleFetchGiftCards: GiftCardActions.FETCH_GIFT_CARDS,
      handleFailedGiftCards: GiftCardActions.GIFT_CARDS_FAILED
    });
  }

  handleUpdateGiftCards(cards) {
    // TODO: Get difference in cards and set 'new' property.
    this.giftCards = cards;
  }

  handleFetchGiftCards() {
    // Fetching gift cards.
  }

  handleFailedGiftCards(err) {
    console.error(err);
  }
}

module.exports = alt.createStore(GiftCardStore, 'GiftCardStore');
