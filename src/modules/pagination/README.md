# Pagination & fetch logic for redux modules

## The concepts

What is pagination mean ? Actually that functionality is for synchronizing some list of entities from server based on some defined criteria.
For example you have list of orders. List has maximum elements to show per each page. Later you can want:

1. Show another page.
2. Add some new entity from the list.
3. Remove some entity from the list.
4. Clear list entirely.

So, pagination it's important part of underlying functionality, but not complete.

## Architecture

Core functionality located in `base.js`.
There are two methods `makePagination` which creates actions and reducer
and `makeFetchAction` which creates fetch actions and used inside `makePagination`.


`makeFetchAction(fetcher, actions, findSearchState)` creates fetch action what calls `fetcher` method with passed
arguments and adds one extra -> `{searchState, getState}` (schema: `fetch(...args) -> fetcher(...args, {searchState, getState}).`
`searchState` is provided by `findSearchState` and `getState` is accessor for root redux state.

In `makePagination(namespace, fetcher)` method `makeFetchAction` called with `state => _.get(state, namespace)` value for
`findSearchState` argument, that means actions will lookup needed state in that place.

In `index.js` located helper function `makePagination(url, namespace, reducer) which chooses `fetcher` for you.
You can see code for discover this stuff, it's simple.

`namespace` is path to `searchData` relatively root level of redux state.

## Intent of fetch

Main intent of `fetch` action is fetch _actual_ data for _current_ state.
So, that's why there is `{searchState, getState}` in last argument for fetch method.

You can checkout `live-search/searches-data.js` code to see usage of `makeFetchAction` method for live-search purposes.

## Use cases

### Static url

In that case you have static url (`/orders` for example) and flat store for underlying data.

Usage example:

```es6
import makePagination from '../pagination';

const { reducer, fetch } = makePagination('/orders', 'orders');

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

### Dynamic url

In that case you have dynamic url (`/gift-cards/:id/transactions` for example) and flat store for underlying data.

Ok, let's see example

```es6
import makePagination from '../pagination';

const { reducer, fetch, resetSearch } = makePagination(
  id => `/gift-cards/${id}/transactions`,
  'giftCards'
);

export {
  reducer as default,
  fetch as fetchTransactions,
  resetSearch
}
```

There are two differences.
First you should reset redux state each time when you mount your component.
Because you have flat store, you can't be sure that store is empty.
And second, instead of static url you define method that accepts some argument and returns url.

Oh, and third. Interface for fetch is `fetch(entity)` in this case.
`entity` is anything that you see fit as argument to the function that creates URL.

## TableView component

TableView is component for rendering and manipulate some list-like data.

TableView in terms of this article accepts two arguments:

- `data` - data that pagination reducer creates.
- `setState` - method that invoked with new fetchParams, usually you should call `updateStateAndFetch` action there.

Example:

```jsx

@connect((state, props) => ({transactions: state.giftCards.transactions}), GiftCardTransactionsActions)
class Transactions extends React.Component {

  componentDidMount() {
    this.props.actionReset();
    this.props.fetch(this.giftCardId);
  }

  render() {
    return (
      <TableView
        data={this.props.transactions}
        setState={this.props.updateStateAndFetch}
    );
  }
}
```
