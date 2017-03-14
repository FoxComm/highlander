/* @flow weak */

import { api as foxApi } from '../lib/api';
import { createAsyncActions } from '@foxcomm/wings';
import _ from 'lodash';

export type CrossSellPoint = {
  custID: number,
  prodID: number,
  chanID: number,
};

export const train = (customerId: number, channelId: number, cartLineItemsSkus: Array<any>) => {
  const crossSellPoints = _.map(_(cartLineItemsSkus).map('productFormId').value(), (productFormId) => {
    return { 'custID': customerId, 'prodID': productFormId, 'chanID': channelId };
  });

  return foxApi.crossSell.crossSellTrain({ 'points': crossSellPoints });
};
