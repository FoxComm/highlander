package models

import "fmt"

// TransactionUpdates is an aggregration of all the updates that are happening
// to a set of StockItems during a single transaction.
type TransactionUpdates struct {
	stockItems map[uint]*transactionUpdate
}

func NewTransactionUpdates() *TransactionUpdates {
	return &TransactionUpdates{
		stockItems: map[uint]*transactionUpdate{},
	}
}

// AddUpdate adds a single StockItemTransaction to the list of updates. If an
// update to the specified stock item, unit type, and unit status is already
// specified, this update will get merged in. Otherwise, a new one will be created.
func (t *TransactionUpdates) AddUpdate(stockItemID uint, txn *StockItemTransaction) error {
	txnUpdate, ok := t.stockItems[stockItemID]
	if !ok {
		txnUpdate = newTransactionUpdate()
	}

	if err := txnUpdate.UpdateTransaction(txn); err != nil {
		return err
	}

	t.stockItems[stockItemID] = txnUpdate
	return nil
}

// StockItemTransactions retrieves the flat list of all StockItemTransactions
// that have been associated with this update.
func (t *TransactionUpdates) StockItemTransactions() []*StockItemTransaction {
	txns := []*StockItemTransaction{}
	for _, txnUpdates := range t.stockItems {
		txns = append(txns, txnUpdates.StockItemTransactions()...)
	}
	return txns
}

type transactionUpdate struct {
	Sellable    map[UnitStatus]*StockItemTransaction
	NonSellable map[UnitStatus]*StockItemTransaction
	Backorder   map[UnitStatus]*StockItemTransaction
	Preorder    map[UnitStatus]*StockItemTransaction
}

func newTransactionUpdate() *transactionUpdate {
	return &transactionUpdate{
		Sellable:    map[UnitStatus]*StockItemTransaction{},
		NonSellable: map[UnitStatus]*StockItemTransaction{},
		Backorder:   map[UnitStatus]*StockItemTransaction{},
		Preorder:    map[UnitStatus]*StockItemTransaction{},
	}
}

func (t *transactionUpdate) UpdateTransaction(txn *StockItemTransaction) error {
	switch txn.Type {
	case Sellable:
		t.Sellable = updateTransactionStatus(txn, t.Sellable)
	case NonSellable:
		t.NonSellable = updateTransactionStatus(txn, t.NonSellable)
	case Backorder:
		t.Backorder = updateTransactionStatus(txn, t.Backorder)
	case Preorder:
		t.Preorder = updateTransactionStatus(txn, t.Preorder)
	default:
		return fmt.Errorf("Updating transaction failed with unexpected unit type %s", txn.Type)
	}

	return nil
}

func (t *transactionUpdate) StockItemTransactions() []*StockItemTransaction {
	txns := []*StockItemTransaction{}

	for _, txn := range t.Sellable {
		txns = append(txns, txn)
	}

	for _, txn := range t.NonSellable {
		txns = append(txns, txn)
	}

	for _, txn := range t.Backorder {
		txns = append(txns, txn)
	}

	for _, txn := range t.Preorder {
		txns = append(txns, txn)
	}

	return txns
}

func updateTransactionStatus(txn *StockItemTransaction, txns map[UnitStatus]*StockItemTransaction) map[UnitStatus]*StockItemTransaction {
	existingTxn, ok := txns[txn.Status]
	if !ok {
		txns[txn.Status] = txn
	} else {
		existingTxn.QuantityNew += txn.QuantityNew
		existingTxn.QuantityChange += txn.QuantityChange
		existingTxn.AFSNew += txn.AFSNew
	}

	return txns
}
