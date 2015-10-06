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
      handleFailedGiftCards: GiftCardActions.GIFT_CARDS_FAILED
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
}

export default alt.createStore(GiftCardStore, 'GiftCardStore');
