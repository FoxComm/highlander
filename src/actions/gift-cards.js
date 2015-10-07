'use strict';

import alt from '../alt';
import Api from '../lib/api';

class GiftCardActions {
  updateGiftCards(cards) {
    this.dispatch(cards);
  }

  fetchGiftCards() {
    this.dispatch();
    return Api.get('/gift-cards')
      .then((cards) => {
        this.actions.updateGiftCards(cards);
      })
      .catch((err) => {
        this.actions.giftCardsFailed(err);
      });
  }

  fetchGiftCard(id) {
    this.dispatch();
    Api.get(`/gift-cards/${id}`)
      .then((card) => {
        this.actions.updateGiftCards([card]);
      })
      .catch((err) => {
        this.actions.giftCardsFailed(err);
      });
  }

  giftCardsFailed(errorMessage) {
    this.dispatch(errorMessage);
  }

  createGiftCard(form) {
    this.dispatch();
    return Api.submitForm(form)
      .then((cards) => {
        this.actions.updateGiftCards(cards);
      })
      .catch((err) => {
        this.actions.giftCardsFailed(err);
      });
  }

  editGiftCard(id, data) {
    this.dispatch();
    return Api.patch(`/gift-cards/${id}`, data)
      .then((card) => {
        this.actions.updateGiftCards([card]);
      })
      .catch((err) => {
        this.actions.giftCardsFailed(err);
      });
  }
}

export default alt.createActions(GiftCardActions);
