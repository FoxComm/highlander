'use strict';

import alt from '../alt';
import Api from '../lib/api';

class GiftCardActions {
  updateGiftCards(cards) {
    this.dispatch(cards);
  }

  fetchGiftCards() {
    this.dispatch();
    Api.get('/gift-cards')
      .then((cards) => {
        this.actions.updateGiftCards(cards);
      })
      .catch((err) => {
        this.actions.giftCardsFailed(err);
      });
  }

  giftCardsFailed(errorMessage) {
    this.dispatch(errorMessage);
  }
}

module.exports = alt.createActions(GiftCardActions);
