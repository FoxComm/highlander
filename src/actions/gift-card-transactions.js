import Api from '../lib/api';
import AshesDispatcher from '../lib/dispatcher';
import GiftCardTransactionConstants from '../constants/gift-card-transactions';
import { List } from 'immutable';

class GiftCardTransactionActions {
  updateTransactions(id, transactions) {
    AshesDispatcher.handleAction({
      actionType: GiftCardTransactionConstants.UPDATE_TRANSACTIONS,
      giftCard: id,
      transactions: transactions
    });
  }

  failedTransactions(errorMessage) {
    AshesDispatcher.handleAction({
      actionType: GiftCardTransactionConstants.FAILED_TRANSACTIONS,
      errorMessage: errorMessage
    });
  }

  fetchTransactions(id) {
    return Api.get(`/gift-cards/${id}/transactions`)
      .then((transactions) => {
        this.updateTransactions(id, List([transactions]));
      })
      .catch((err) => {
        this.failedTransactions(err);
      });
  }
}

export default new GiftCardTransactionActions();
