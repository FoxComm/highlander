'use strict';

import Api from '../lib/api';
import AshesDispatcher from '../lib/dispatcher';
import GiftCardTransactionConstants from '../constants/gift-card-transactions';
import { List } from 'immutable';

class GiftCardTransactionActions {
  updateTransactions(transactions) {
    AshesDispatcher.handleViewAction({
      actionType: GiftCardTransactionConstants.UPDATE_TRANSACTIONS,
      transactions: transactions
    });
  }

  failedTransactions(errorMessage) {
    AshesDispatcher.handleViewAction({
      actionType: GiftCardTransactionConstants.FAILED_TRANSACTIONS,
      errorMessage: errorMessage
    });
  }

  fetchTransactions(id) {
    return Api.get(`/gift-cards/${id}/transactions`)
      .then((transactions) => {
        this.updateTransactions(List([transactions]));
      })
      .catch((err) => {
        this.failedTransactions(err);
      });
  }
}

export default new GiftCardTransactionActions();
