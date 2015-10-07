'use strict';

import alt from '../alt';
import GiftCardActions from '../actions/gift-cards';
import immutable from 'alt/utils/ImmutableUtil';
import Immutable from 'immutable';

@immutable
class GiftCardStore {
  constructor() {
    this.state = {
      giftCards: Immutable.List([])
    };

    this.bindListeners({
      handleUpdateGiftCards: GiftCardActions.UPDATE_GIFT_CARDS,
      handleFetchGiftCards: GiftCardActions.FETCH_GIFT_CARDS,
      handleFailedGiftCards: GiftCardActions.GIFT_CARDS_FAILED,
      handleFetchGiftCard: GiftCardActions.FETCH_GIFT_CARD,
      handleEditGiftCard: GiftCardActions.EDIT_GIFT_CARD,
      handleCreateGiftCard: GiftCardActions.CREATE_GIFT_CARD
    });
  }

  handleUpdateGiftCards(cards) {
    // TODO: Get difference in cards and set 'new' property.
    this.setState({
      giftCards: Immutable.List(cards)
    });
  }

  handleFetchGiftCards() {
    // Fetching gift cards.
  }

  handleFailedGiftCards(err) {
    console.error(err);
  }

  handleFetchGiftCard() {
    // Fetching gift card.
  }

  handleEditGiftCard() {
    // Editing gift card.
  }

  handleCreateGiftCard() {
    // Creating gift card.
  }
}

export default alt.createStore(GiftCardStore, 'GiftCardStore');
