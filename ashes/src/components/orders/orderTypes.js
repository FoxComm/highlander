// @flow

import type { State as ReduxOrderState } from 'modules/orders/details';
import OrderParagon from 'paragons/order';

export type StateToProps = {
  details: Object;
  isFetching: boolean;
  fetchError: any;
};

export type DispatchToProps = {
  updateOrder: Function;
  updateShipments: Function;
  fetchOrder: Function;
  clearFetchErrors: Function;
  increaseRemorsePeriod: Function;
  fetchAmazonOrder: Function;
};

export type OwnProps = {
  children: any; // @todo
  params: Object;
  route: Object;
};

export type Props = OwnProps & StateToProps & DispatchToProps;

export type StateType = {
  newOrderState: ?string;
};

export type OrderType = OrderParagon;

export type ReduxState = {
  orders: {
    details: ReduxOrderState;
  };
  asyncActions: any;
};
