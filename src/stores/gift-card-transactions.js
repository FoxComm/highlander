import GiftCardTransactionConstants from '../constants/gift-card-transactions';
import { List, Map } from 'immutable';
import BaseStore from './base-store';

class GiftCardTransactionStore extends BaseStore {
  constructor() {
    super();
    this.changeEvent = 'change-gift-card-transactions';
    this.state = List([]);

    this.bindListener(GiftCardTransactionConstants.UPDATE_TRANSACTIONS, this.handleUpdateTransactions);
    this.bindListener(GiftCardTransactionConstants.FAILED_TRANSACTIONS, this.handleFailedTransactions);
  }

  handleUpdateTransactions(action) {
    let existingIndex = this.state.findIndex(item => item.giftCard === action.giftCard);
    if (existingIndex === -1) existingIndex = this.state.size;
    this.setState(this.state.set(existingIndex, Map({
      giftCard: action.giftCard,
      transactions: action.transactions
    })));
  }

  handleFailedTransactions(action) {
    console.error(action.errorMessage.trim());
  }
}

let giftCardTransactionStore = new GiftCardTransactionStore();
export default giftCardTransactionStore;
