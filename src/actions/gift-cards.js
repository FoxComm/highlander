'use strict';

import Api from '../lib/api';
import AshesDispatcher from '../lib/dispatcher';
import GiftCardConstants from '../constants/gift-card';
import { List } from 'immutable';

class GiftCardActions {
  updateGiftCards(giftCards) {
    AshesDispatcher.handleViewAction({
      actionType: GiftCardConstants.UPDATE_GIFT_CARDS,
      giftCards: giftCards
    });
  }

  insertGiftCard(giftCard) {
    AshesDispatcher.handleViewAction({
      actionType: GiftCardConstants.INSERT_GIFT_CARD,
      giftCard: giftCard
    });
  }

  fetchGiftCards() {
    return Api.get('/gift-cards')
      .then((cards) => {
        this.updateGiftCards(List(cards));
      })
      .catch((err) => {
        this.giftCardsFailed(err);
      });
  }

  fetchGiftCard(id) {
    return Api.get(`/gift-cards/${id}`)
      .then((card) => {
        this.insertGiftCard(card);
      })
      .catch((err) => {
        this.giftCardsFailed(err);
      });
  }

  giftCardsFailed(errorMessage) {
    AshesDispatcher.handleViewAction({
      actionType: GiftCardConstants.GIFT_CARD_FAILED,
      errorMessage: errorMessage
    });
  }

  createGiftCard(form) {
    return Api.submitForm(form)
      .then((cards) => {
        this.updateGiftCards(List([cards]));
      })
      .catch((err) => {
        this.giftCardsFailed(err);
      });
  }

  editGiftCard(id, data) {
    return Api.patch(`/gift-cards/${id}`, data)
      .then((card) => {
        this.updateGiftCards(List([card]));
      })
      .catch((err) => {
        this.giftCardsFailed(err);
      });
  }
}

export default new GiftCardActions();
