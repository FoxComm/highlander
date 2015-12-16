# Pagination & fetch logic for redux modules

## The concepts

What is pagination mean ? Actually that functionality is for synchronizing data from server based on some defined criteria.
For example you have list of orders. List has maximum elements to show per each page. Later you can want:

1. Show another page.
2. Add some new entity from the list.
3. Remove some entity from the list.
4. Clear list entirely.

So, pagination it's important part of underlying functionality, but not complete.

## Architecture

Core functionality located in `base.js` and this functionality designed for maximum flexibility.

Quick introduction to entities in `base.js` module:

### First Layer

##### actionTypes

Enum that contains action types for pagination/fetching purposes.
Each action type in enum is just string.


##### paginate(state, action)

Main reducer that accepts action from `actionTypes` enum and update state for us.

### Second Layer

Ok, we have action types and reducer. But our reducer can't handle all lists in app.
So, it's time for namespaces and actions creators.

##### createFetchActions(namespace, payloadReducer, metaReducer)

Creates action in terms of `redux-act` for each action type enumerated in `actionTypes`.
namespace is necessary to distinguish different actions between different reducers.

##### paginateReducer(namespace, reducer, updateBehaviour)

It's high order reducer that adds ability to handle pagination actions to your reducer.

### Async actions

There is `createActions` function in `actions-creator.js` file.

Except actions from `createFetchActions` it creates `fetch` async action.
We will not consider in detail it here, if you want you can see the source code.

## Use cases

### Static url, flat store

In that case you have static url (`/orders` for example) and flat store for underlying data.

Usage example:

```es6
import makePagination from '../pagination';

const { reducer, fetch } = makePagination('/orders', 'ORDERS');

export {
  reducer as default,
  fetch as fetchOrders
}
```

That's all. You don't have any specific actions & reducer logic for your module. But you can do it:

```es6
import makePagination from '../pagination';

export const fireAction = createAction(...);
const ordersReducer = createReducer(...);

const { reducer, fetch } = makePagination('/orders', 'ORDERS', ordersReducer);

export {
  reducer as default,
  fetch as fetchOrders,
  fireAction
}
```

Interface for fetch is `fetch(fetchParams={})` in this case.

### Dynamic url, flat store

In that case you have dynamic url (`/gift-cards/:id/transactions` for example) and flat store for underlying data.
I.e. you don't want store different transactions from different gift cards at the same time.
Seriously, do you really need it ?

Ok, let's see example

```es6
import makePagination from '../pagination';

const { reducer, fetch, actionReset } = makePagination(
  id => `/gift-cards/${id}/transactions`,
  'GIFTCARD_TRANSACTIONS'
);

export {
  reducer as default,
  fetch as fetchTransactions,
  actionReset
}
```

There are two differences.
First you should reset redux state each time when you mount your component.
Because you have flat store, you can't be sure that store is empty.
And second, instead of static url you define method that accepts some argument and returns url.

Oh, and third. Interface for fetch is `fetch(entity, fetchParams={})` in this case.
`entity` is anything that you see fit as argument to the function that creates URL.

### Dynamic url, structured store

In that case you have dynamic url (`/notes/:type/:id` for example) and some structured store for underlying data.
I.e. you _want_ to store different notes from different entities at the same time.

Ok, let's see example, it will be a little harder:

```es6

import makePagination from './pagination/structured-store';

const dataPath = ({entityType, entityId}) => [entityType, entityId];
const { makeActions, makeReducer } = makePagination('NOTES', dataPath);

export const notesUri = entity => `/notes/${entity.entityType}/${entity.entityId}`;

const { fetch, actionAddEntity, ... } = makeActions(notesUri);

const notesReducer = createReducer(...)

const reducer = makeReducer(notesReducer);

export {
  reducer as default,
  fetch as fetchNotes,
  ...
}
```

Note that you import `makePagination` from different path.

`dataPath` is function that accepts an `entity` and should return path as array where data and pagination info will be saved.

Interface for fetch is `fetch(entity, fetchParams={})`.

## TableView component

TableView is component for rendering and manipulate some list-like data.

TableView in terms of this article accepts two arguments:

- `data` - data that pagination reducer creates.
- `setState` - method that invoked with new fetchParams, usually you should call fetch there.

Example:

```jsx

@connect((state, props) => ({transactions: state.giftCards.transactions}), GiftCardTransactionsActions)
class Transactions extends React.Component {

  componentDidMount() {
    this.props.actionReset();
    this.props.fetchTransaction(this.giftCardId);
  }

  render() {
    return (
      <TableView
        data={this.props.transactions}
        setState={params => this.props.fetchTransaction(this.giftCardId)}
    );
  }
}
```
